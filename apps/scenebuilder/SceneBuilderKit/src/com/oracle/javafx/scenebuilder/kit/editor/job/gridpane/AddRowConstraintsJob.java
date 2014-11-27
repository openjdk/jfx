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
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchDocumentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.JobUtils;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.GridPaneJobUtils.Position;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.layout.RowConstraints;

/**
 * Job invoked when adding row constraints.
 */
public class AddRowConstraintsJob extends BatchDocumentJob {

    // Key = target GridPane instance
    // Value = list of target row indexes for this GridPane
    private final Map<FXOMObject, Set<Integer>> targetGridPanes;
    private final Position position;
    // If the selected row is associated to an existing constraints, 
    // we duplicate the existing constraints.
    // Otherwise, we use the default values below.
    private static final double defaultMinHeight = 10.0;
    private static final double defaultPrefHeight = 30.0;

    public AddRowConstraintsJob(
            final EditorController editorController,
            final Position position,
            final Map<FXOMObject, Set<Integer>> targetGridPanes) {
        super(editorController);
        this.position = position;
        this.targetGridPanes = targetGridPanes;
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();

        // Add column constraints job
        assert targetGridPanes.isEmpty() == false;
        for (FXOMObject targetGridPane : targetGridPanes.keySet()) {
            assert targetGridPane instanceof FXOMInstance;
            final Set<Integer> targetIndexes = targetGridPanes.get(targetGridPane);
            result.addAll(addRowConstraints((FXOMInstance) targetGridPane, targetIndexes));
        }
        
        return result;
    }
    
    @Override
    protected String makeDescription() {
        return "Add Row Constraints"; //NOI18N
    }

    private Set<Job> addRowConstraints(
            final FXOMInstance targetGridPane,
            final Set<Integer> targetIndexes) {

        final Set<Job> result = new LinkedHashSet<>();
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        // Retrieve the constraints property for the specified target GridPane
        final PropertyName propertyName = new PropertyName("rowConstraints"); //NOI18N
        FXOMProperty constraintsProperty = targetGridPane.getProperties().get(propertyName);
        if (constraintsProperty == null) {
            constraintsProperty = new FXOMPropertyC(fxomDocument, propertyName);
        }
        assert constraintsProperty instanceof FXOMPropertyC;

        final DesignHierarchyMask mask = new DesignHierarchyMask(targetGridPane);

        int shiftIndex = 0;
        int constraintsSize = mask.getRowsConstraintsSize();
        for (int targetIndex : targetIndexes) {

            // Retrieve the index for the new constraints to be added
            int addedIndex = targetIndex + shiftIndex;
            if (position == Position.BELOW) {
                addedIndex++;
            }

            final FXOMObject targetConstraints
                    = mask.getRowConstraintsAtIndex(targetIndex);
            // The target index is associated to an existing constraints value :
            // we add a new constraints using the values of the existing one
            if (targetConstraints != null) {
                assert targetConstraints instanceof FXOMInstance;
                // Create new constraints instance with same values as the target one
                final FXOMInstance addedConstraints = makeRowConstraintsInstance(
                        (FXOMInstance) targetConstraints);

                final Job addValueJob = new AddPropertyValueJob(
                        addedConstraints,
                        (FXOMPropertyC) constraintsProperty,
                        addedIndex, getEditorController());
                result.add(addValueJob);
            } //
            // The target index is not associated to an existing constraints value :
            // - we add new empty constraints from the last existing one to the added index (excluded)
            // - we add a new constraints with default values for the added index
            else {
                for (int index = constraintsSize; index < addedIndex; index++) {
                    // Create new empty constraints for the exisiting rows
                    final FXOMInstance addedConstraints = makeRowConstraintsInstance();
                    final Job addValueJob = new AddPropertyValueJob(
                            addedConstraints,
                            (FXOMPropertyC) constraintsProperty,
                            index, getEditorController());
                    result.add(addValueJob);
                }
                // Create new constraints with default values for the new added row
                final FXOMInstance addedConstraints = makeRowConstraintsInstance();
                JobUtils.setMinHeight(addedConstraints, RowConstraints.class, defaultMinHeight);
                JobUtils.setPrefHeight(addedConstraints, RowConstraints.class, defaultPrefHeight);
                final Job addValueJob = new AddPropertyValueJob(
                        addedConstraints,
                        (FXOMPropertyC) constraintsProperty,
                        addedIndex, getEditorController());
                result.add(addValueJob);
                constraintsSize = addedIndex + 1;
            }
            shiftIndex++;
        }

        // Add the constraints property to the target GridPane if not already there.
        // IMPORTANT :
        // Note that the AddPropertyJob must be called after the AddPropertyValueJob.
        if (constraintsProperty.getParentInstance() == null) {
            final Job addPropertyJob = new AddPropertyJob(
                    constraintsProperty,
                    targetGridPane,
                    -1, getEditorController());
            result.add(addPropertyJob);
        }
        
        return result;
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

    private FXOMInstance makeRowConstraintsInstance(final FXOMInstance constraints) {

        assert constraints != null;
        assert constraints.getDeclaredClass() == RowConstraints.class;

        // Create new constraints instance
        final FXOMInstance result = makeRowConstraintsInstance();

        // Set the new row constraints values with the values of the specified instance
        final boolean fillHeight = JobUtils.getFillHeight(constraints, RowConstraints.class);
        final double maxHeight = JobUtils.getMaxHeight(constraints, RowConstraints.class);
        final double minHeight = JobUtils.getMinHeight(constraints, RowConstraints.class);
        final double percentHeight = JobUtils.getPercentHeight(constraints, RowConstraints.class);
        final double prefHeight = JobUtils.getPrefHeight(constraints, RowConstraints.class);
        final String valignment = JobUtils.getVAlignment(constraints, RowConstraints.class);
        final String vgrow = JobUtils.getVGrow(constraints, RowConstraints.class);

        JobUtils.setFillHeight(result, RowConstraints.class, fillHeight);
        JobUtils.setMaxHeight(result, RowConstraints.class, maxHeight);
        // If the existing constraints minHeight is too small, we use the default one
        JobUtils.setMinHeight(result, RowConstraints.class, Math.max(minHeight, defaultMinHeight));
        JobUtils.setPercentHeight(result, RowConstraints.class, percentHeight);
        // If the existing constraints prefHeight is too small, we use the default one
        JobUtils.setPrefHeight(result, RowConstraints.class, Math.max(prefHeight, defaultPrefHeight));
        JobUtils.setVAlignment(result, RowConstraints.class, valignment);
        JobUtils.setVGrow(result, RowConstraints.class, vgrow);

        return result;
    }
}

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
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.BackupSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class BatchModifySelectionJob extends CompositeJob {

    private final ValuePropertyMetadata propertyMetadata;
    private final Object newValue;

    public BatchModifySelectionJob(ValuePropertyMetadata propertyMetadata,
            Object newValue, EditorController editorController) {
        super(editorController, true, false);
        this.propertyMetadata = propertyMetadata;
        this.newValue = newValue;
    }

    /*
     * CompositeJob
     */
    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();
        final Set<FXOMInstance> candidates = new HashSet<>();
        final Selection selection = getEditorController().getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            for (FXOMObject fxomObject : osg.getItems()) {
                if (fxomObject instanceof FXOMInstance) {
                    candidates.add((FXOMInstance) fxomObject);
                }
            }
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
            final DesignHierarchyMask mask = new DesignHierarchyMask(gsg.getAncestor());
            for (int index : gsg.getIndexes()) {
                FXOMObject constraints = null;
                switch (gsg.getType()) {
                    case COLUMN:
                        constraints = mask.getColumnConstraintsAtIndex(index);
                        break;
                    case ROW:
                        constraints = mask.getRowConstraintsAtIndex(index);
                        break;
                    default:
                        assert false;
                        break;
                }
                assert constraints instanceof FXOMInstance;
                candidates.add((FXOMInstance) constraints);
            }
        } else {
            assert selection.getGroup() == null : "Add implementation for " + selection.getGroup();
        }

        final Set<FXOMObject> executableCandidates = new HashSet<>();

        // First add ModifyObject jobs
        for (FXOMInstance fxomInstance : candidates) {
            final ModifyObjectJob subJob = new ModifyObjectJob(
                    fxomInstance, propertyMetadata, newValue, getEditorController());
            if (subJob.isExecutable()) {
                result.add(subJob);
                executableCandidates.add(fxomInstance);
            }
        }

        // If the list of ModifyObject jobs is not empty,
        // then add selection specific jobs
        if (result.isEmpty() == false) {
            result.add(0, new BackupSelectionJob(getEditorController()));
            result.add(new UpdateSelectionJob(executableCandidates, getEditorController()));
        }

        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
        final List<Job> subJobs = getSubJobs();
        final int subJobCount = subJobs.size();
        assert (subJobCount == 0) || (subJobCount >= 3);

        switch (subJobCount) {
            case 0:
                result = "Unexecutable Set"; //NOI18N
                break;
            case 3: // BackupSelection + 1 ModifyObject + UpdateSelection
                result = subJobs.get(1).getDescription();
                break;
            default:
                result = I18N.getString("label.action.edit.set.n",
                        propertyMetadata.getName().toString(),
                        subJobCount - 2); // Remove 2 selection jobs (BackupSelection + UpdateSelection) 
                break;
        }

        return result;
    }
}

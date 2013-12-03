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
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class SendBackwardJob extends ArrangeZOrderJob {

    public SendBackwardJob(EditorController editorController) {
        super(editorController);
        buildSubJobs();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            buildDescription();
        }
        return description;
    }

    /*
     * Private
     */
    private void buildSubJobs() {

        final Set<FXOMObject> candidates = new HashSet<>();
        final Selection selection = getEditorController().getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            candidates.addAll(osg.getFlattenItems());
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            // GridPane rows / columns are selected : SendBackwardJob is meaningless
            // Just do nothing
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup();
        }

        for (FXOMObject candidate : candidates) {
            final FXOMObject previousSlibing = candidate.getPreviousSlibing();
            if (previousSlibing != null) {
                final ReIndexObjectJob subJob = new ReIndexObjectJob(
                        candidate, previousSlibing, getEditorController());
                if (subJob.isExecutable()) {
                    subJobs.add(subJob);
                }
            }
        }
    }

    private void buildDescription() {
        switch (subJobs.size()) {
            case 0:
                description = "Unexecutable Send Backward"; // NO18N
                break;
            case 1:
                description = subJobs.get(0).getDescription();
                break;
            default:
                description = makeMultipleSelectionDescription();
                break;
        }
    }

    private String makeMultipleSelectionDescription() {
        final StringBuilder result = new StringBuilder();
        result.append("Send Backward ");
        result.append(subJobs.size());
        result.append(" Objects");
        return result.toString();
    }
}

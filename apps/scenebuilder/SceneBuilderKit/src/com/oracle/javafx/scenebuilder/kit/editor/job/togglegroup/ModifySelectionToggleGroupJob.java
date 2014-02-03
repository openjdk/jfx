/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.ClearSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ToggleGroup;

/**
 *
 */
public class ModifySelectionToggleGroupJob extends CompositeJob {

    private final String toggleGroupId;

    public ModifySelectionToggleGroupJob(String toggleGroupId, EditorController editorController) {
        super(editorController);
        
        assert editorController.getFxomDocument() != null;
        
        this.toggleGroupId = toggleGroupId;
    }
    
    
    /*
     * CompositeJob
     */
    
    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        
        /*
         * Checks that toggleGroupId is:
         *  0) either null
         *  1) either an unused fx:id
         *  2) either the fx:id of an existing ToggleGroup instance
         */
        
        final boolean executable;
        if (toggleGroupId == null) {
            executable = true;
        } else {
            final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
            final FXOMObject toggleGroupObject = fxomDocument.searchWithFxId(toggleGroupId);
            if (toggleGroupObject == null) {
                // Case #1
                executable = true;
            } else if (toggleGroupObject instanceof FXOMInstance) {
                // Case #2
                final FXOMInstance toggleGroupInstance = (FXOMInstance) toggleGroupObject;
                executable = toggleGroupInstance.getDeclaredClass() == ToggleGroup.class;
            } else {
                executable = false;
            }
        }
        
        /*
         * Creates some ModifyToggleGroupJob instances
         */
        if (executable) {
            final Selection selection = getEditorController().getSelection();
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                for (FXOMObject fxomObject : osg.getItems()) {
                    final Job subJob
                            = new ModifyToggleGroupJob(fxomObject, toggleGroupId, getEditorController());
                    if (subJob.isExecutable()) {
                        result.add(subJob);
                    }
                }

                if (result.isEmpty() == false) {
                    result.add(0, new ClearSelectionJob(getEditorController()));
                    result.add(new AdjustAllToggleGroupJob(getEditorController()));
                    result.add(new UpdateSelectionJob(osg.getItems(), getEditorController()));
                }
            }
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        return I18N.getString("job.set.toggle.group");
    }
    
}

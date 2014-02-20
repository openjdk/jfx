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
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.BackupSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BatchModifyFxIdJob extends CompositeJob {

    private final FXOMObject fxomObject;
    private final String newValue;

    public BatchModifyFxIdJob(
            FXOMObject fxomObject, 
            String newValue,
            EditorController editorController) {
        super(editorController);

        assert fxomObject != null;

        this.fxomObject = fxomObject;
        this.newValue = newValue;
    }

    /*
     * CompositeJob
     */
    
    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        
        final ModifyFxIdJob job = new ModifyFxIdJob(fxomObject, newValue, getEditorController());
        if (job.isExecutable()) {
            result.add(job);
        }
        
        if (result.isEmpty() == false) {
            result.add(0, new BackupSelectionJob(getEditorController()));
            result.add(new UpdateSelectionJob(fxomObject, getEditorController()));
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
        final List<Job> subJobs = getSubJobs();
        final int subJobCount = subJobs.size();
        assert (subJobCount == 0) || (subJobCount == 3);

        if (subJobCount == 0) {
            result = "Unexecutable Set"; //NOI18N
        } else {
            result = subJobs.get(1).getDescription(); // BackupSelection + 1 ModifyFxId + UpdateSelection
        }

        return result;
    }
}

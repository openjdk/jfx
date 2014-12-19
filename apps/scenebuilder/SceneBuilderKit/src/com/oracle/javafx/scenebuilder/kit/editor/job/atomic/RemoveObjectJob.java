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

package com.oracle.javafx.scenebuilder.kit.editor.job.atomic;

import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemoveCollectionItemJob;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 *
 */
public class RemoveObjectJob extends Job {
    
    private final Job subJob;
    
    public RemoveObjectJob(FXOMObject targetObject, EditorController editorController) {
        super(editorController);
        
        assert targetObject != null;
        assert (targetObject.getParentProperty() != null) || (targetObject.getParentCollection() != null);
        
        if (targetObject.getParentProperty() != null) {
            subJob = new RemovePropertyValueJob(targetObject, editorController);
        } else {
            assert targetObject.getParentCollection() != null;
            subJob = new RemoveCollectionItemJob(targetObject, editorController);
        }
    }
    
    
    /*
     * Job
     */

    @Override
    public boolean isExecutable() {
        return subJob.isExecutable();
    }

    @Override
    public void execute() {
        subJob.execute();
    }

    @Override
    public void undo() {
        subJob.undo();
    }

    @Override
    public void redo() {
        subJob.redo();
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName(); // Should not reach end user
    }
    
}

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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 *
 */
public class ReIndexObjectJob extends Job {

    private final FXOMObject reindexedObject;
    private final FXOMObject beforeObject;
    private final FXOMObject oldBeforeObject;
    private String description;
    

    public ReIndexObjectJob(
            FXOMObject reindexedObject, 
            FXOMObject beforeObject, 
            EditorController editorController) {
        super(editorController);
        assert reindexedObject != null;
        
        this.reindexedObject = reindexedObject;
        this.beforeObject = beforeObject;
        this.oldBeforeObject = reindexedObject.getNextSlibing();
    }
    
    
    /*
     * Job
     */
    
    @Override
    public boolean isExecutable() {
        return (beforeObject != oldBeforeObject);
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void undo() {
        assert isExecutable();

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        reindexedObject.moveBeforeSibling(oldBeforeObject);
        fxomDocument.endUpdate();
    }

    @Override
    public void redo() {
        assert isExecutable();

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        reindexedObject.moveBeforeSibling(beforeObject);
        fxomDocument.endUpdate();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            final StringBuilder sb = new StringBuilder();

            sb.append("Move ");

            if (reindexedObject instanceof FXOMInstance) {
                final Object sceneGraphObject = reindexedObject.getSceneGraphObject();
                if (sceneGraphObject != null) {
                    sb.append(sceneGraphObject.getClass().getSimpleName());
                } else {
                    sb.append("Unresolved Object");
                }
            } else if (reindexedObject instanceof FXOMCollection) {
                sb.append("Collection");
            } else {
                assert false;
                sb.append(reindexedObject.getClass().getSimpleName());
            }
            description = sb.toString();
        }
        return description;
    }
    
}

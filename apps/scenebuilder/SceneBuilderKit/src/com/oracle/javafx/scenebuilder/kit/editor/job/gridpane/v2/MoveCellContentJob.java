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

package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.IntegerPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/**
 *
 */
public class MoveCellContentJob extends Job {
    
    private static final IntegerPropertyMetadata columnIndexMeta =
            new IntegerPropertyMetadata(
                new PropertyName("columnIndex", GridPane.class), //NOI18N
                true, /* readWrite */
                0, /* defaultValue */
                InspectorPath.UNUSED);
    private static final IntegerPropertyMetadata rowIndexMeta =
            new IntegerPropertyMetadata(
                new PropertyName("rowIndex", GridPane.class), //NOI18N
                true, /* readWrite */
                0, /* defaultValue */
                InspectorPath.UNUSED);
    
    private final FXOMInstance fxomObject;
    private final int columnIndexDelta;
    private final int rowIndexDelta;
    private int oldColumnIndex = -1;
    private int oldRowIndex = -1;

    public MoveCellContentJob(FXOMInstance fxomObject, 
            int columnIndexDelta, int rowIndexDelta, 
            EditorController editorController) {
        super(editorController);
        assert fxomObject != null;
        assert fxomObject.getSceneGraphObject() instanceof Node;
        
        this.fxomObject = fxomObject;
        this.columnIndexDelta = columnIndexDelta;
        this.rowIndexDelta = rowIndexDelta;
    }

    /*
     * Job
     */
    
    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public void execute() {
        oldColumnIndex = columnIndexMeta.getValue(fxomObject);
        oldRowIndex = rowIndexMeta.getValue(fxomObject);
        
        assert oldColumnIndex + columnIndexDelta >= 0;
        assert oldRowIndex + rowIndexDelta >= 0;
        
        // Now same as redo()
        redo();
    }

    @Override
    public void undo() {
        assert isExecutable();

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        columnIndexMeta.setValue(fxomObject, oldColumnIndex);
        rowIndexMeta.setValue(fxomObject, oldRowIndex);
        fxomDocument.endUpdate();
    }

    @Override
    public void redo() {
        assert isExecutable();

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        columnIndexMeta.setValue(fxomObject, oldColumnIndex + columnIndexDelta);
        rowIndexMeta.setValue(fxomObject, oldRowIndex + rowIndexDelta);
        fxomDocument.endUpdate();
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName();
    }
    
}

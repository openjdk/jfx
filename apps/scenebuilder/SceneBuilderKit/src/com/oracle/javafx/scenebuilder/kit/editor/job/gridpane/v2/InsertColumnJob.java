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
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ColumnConstraintsListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.GridPane;

/**
 *
 */
public class InsertColumnJob extends BatchSelectionJob {

    private static final ColumnConstraintsListPropertyMetadata columnContraintsMeta =
            new ColumnConstraintsListPropertyMetadata(
                new PropertyName("columnConstraints"), //NOI18N
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                InspectorPath.UNUSED);

    private final FXOMInstance gridPaneObject;
    private final int columnIndex;
    private final int insertCount;

    public InsertColumnJob(FXOMObject gridPaneObject, 
            int columnIndex, int insertCount, EditorController editorController) {
        super(editorController);
        
        assert gridPaneObject instanceof FXOMInstance;
        assert gridPaneObject.getSceneGraphObject() instanceof GridPane;
        assert columnIndex >= 0;
        assert columnIndex <= columnContraintsMeta.getValue((FXOMInstance)gridPaneObject).size();
        assert insertCount >= 1;
        
        this.gridPaneObject = (FXOMInstance)gridPaneObject;
        this.columnIndex = columnIndex;
        this.insertCount = insertCount;
    }

    /*
     * CompositeJob
     */
    
    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        
        final Job insertJob 
                = new InsertColumnConstraintsJob(gridPaneObject, columnIndex, insertCount, getEditorController());
        result.add(insertJob);
        
        final int lastColumnIndex = columnContraintsMeta.getValue(gridPaneObject).size()-1;
        for (int c = lastColumnIndex; c >= columnIndex; c--) {
            final Job moveJob
                    = new MoveColumnContentJob(gridPaneObject, c, +insertCount, getEditorController());
            if (moveJob.isExecutable()) {
                result.add(moveJob);
            } // else column is empty : no children to move
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        return getClass().getSimpleName();
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        return new GridSelectionGroup(gridPaneObject, GridSelectionGroup.Type.COLUMN, columnIndex);
    }
}

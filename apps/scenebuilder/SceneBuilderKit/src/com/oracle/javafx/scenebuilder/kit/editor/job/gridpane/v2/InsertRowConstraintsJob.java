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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.RowConstraintsListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

/**
 *
 */
public class InsertRowConstraintsJob extends Job {

    private static final RowConstraintsListPropertyMetadata rowContraintsMeta =
            new RowConstraintsListPropertyMetadata(
                new PropertyName("rowConstraints"), //NOI18N
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                InspectorPath.UNUSED);

    private final FXOMInstance gridPaneObject;
    private final int rowIndex;
    private final int insertCount;

    public InsertRowConstraintsJob(FXOMObject gridPaneObject, 
            int rowIndex, int insertCount, EditorController editorController) {
        super(editorController);
        
        assert gridPaneObject instanceof FXOMInstance;
        assert gridPaneObject.getSceneGraphObject() instanceof GridPane;
        assert rowIndex >= 0;
        assert rowIndex <= rowContraintsMeta.getValue((FXOMInstance)gridPaneObject).size();
        assert insertCount >= 1;
        
        this.gridPaneObject = (FXOMInstance)gridPaneObject;
        this.rowIndex = rowIndex;
        this.insertCount = insertCount;
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
        // Same as redo()
        redo();
    }

    @Override
    public void undo() {
        final List<RowConstraints> constraintsList 
                = new ArrayList<>(rowContraintsMeta.getValue(gridPaneObject));
        assert rowIndex < constraintsList.size();
        for (int i = 0; i < insertCount; i++) {
            constraintsList.remove(rowIndex);
        }
        rowContraintsMeta.setValue(gridPaneObject, constraintsList);
    }

    @Override
    public void redo() {
        final List<RowConstraints> constraintsList 
                = new ArrayList<>(rowContraintsMeta.getValue(gridPaneObject));
        final RowConstraints template;
        if (rowIndex >= 1) {
            template = constraintsList.get(rowIndex-1);
        } else {
            template = null;
        }
        for (int i = 0; i < insertCount; i++) {
            constraintsList.add(rowIndex, makeRowConstraints(template));
        }
        rowContraintsMeta.setValue(gridPaneObject, constraintsList);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName();
    }
    
    
    /*
     * Private
     */
    
    private RowConstraints makeRowConstraints(RowConstraints template) {
        final RowConstraints result = new RowConstraints();
        if (rowIndex >= 1) {
            result.setFillHeight(template.isFillHeight());
            result.setValignment(template.getValignment());
            result.setVgrow(template.getVgrow());
            result.setMaxHeight(template.getMaxHeight());
            result.setMinHeight(template.getMinHeight());
            result.setPercentHeight(template.getPercentHeight());
            result.setPrefHeight(template.getPrefHeight());
        }
        return result;
    }
}

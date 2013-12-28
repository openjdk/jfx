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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Control;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

/**
 *
 */
public class UseComputedSizesObjectJob extends Job {

    private final FXOMInstance fxomInstance;
    private final List<ModifyObjectJob> subJobs = new ArrayList<>();
    private String description; // final but initialized lazily

    public UseComputedSizesObjectJob(FXOMInstance fxomInstance, EditorController editorController) {
        super(editorController);
        assert fxomInstance != null;
        this.fxomInstance = fxomInstance;
        buildSubJobs();
    }

    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        return subJobs.isEmpty() == false;
    }

    @Override
    public void execute() {
        final FXOMDocument fxomDocument
                = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        for (ModifyObjectJob subJob : subJobs) {
            subJob.execute();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public void undo() {
        final FXOMDocument fxomDocument
                = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        for (int i = subJobs.size() - 1; i >= 0; i--) {
            subJobs.get(i).undo();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public void redo() {
        final FXOMDocument fxomDocument
                = getEditorController().getFxomDocument();
        fxomDocument.beginUpdate();
        for (ModifyObjectJob subJob : subJobs) {
            subJob.redo();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Use Computed Sizes on ");
            final Object sceneGraphObject = fxomInstance.getSceneGraphObject();
            assert sceneGraphObject != null;
            sb.append(sceneGraphObject.getClass().getSimpleName());
            description = sb.toString();
        }
        return description;
    }

    private void buildSubJobs() {

        final Object sceneGraphObject = fxomInstance.getSceneGraphObject();

        // RowConstraints: only height property is meaningfull
        if (sceneGraphObject instanceof RowConstraints) {
            subJobs.addAll(modifyHeightJobs(fxomInstance));
        } //
        // ColumnConstraints: only width property is meaningfull
        else if (sceneGraphObject instanceof ColumnConstraints) {
            subJobs.addAll(modifyWidthJobs(fxomInstance));
        } //
        // Control/Region: both height and width properties are meaningfull
        else if (sceneGraphObject instanceof Control
                || sceneGraphObject instanceof Region) {
            subJobs.addAll(modifyHeightJobs(fxomInstance));
            subJobs.addAll(modifyWidthJobs(fxomInstance));
        } //
        // Use computed sizes on ImageView => set the fit size to (0,0)
        else if (sceneGraphObject instanceof ImageView) {
            subJobs.addAll(modifyFitHeightJob(fxomInstance));
            subJobs.addAll(modifyFitWidthJob(fxomInstance));
        }
    }

    private List<ModifyObjectJob> modifyHeightJobs(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final PropertyName maxHeight = new PropertyName("maxHeight");
        final PropertyName minHeight = new PropertyName("minHeight");
        final PropertyName prefHeight = new PropertyName("prefHeight");

        final ValuePropertyMetadata maxHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxHeight);
        final ValuePropertyMetadata minHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minHeight);
        final ValuePropertyMetadata prefHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefHeight);

        final ModifyObjectJob maxHeightJob = new ModifyObjectJob(
                candidate, maxHeightVPM, -1.0, getEditorController());
        final ModifyObjectJob minHeightJob = new ModifyObjectJob(
                candidate, minHeightVPM, -1.0, getEditorController());
        final ModifyObjectJob prefHeightJob = new ModifyObjectJob(
                candidate, prefHeightVPM, -1.0, getEditorController());

        if (maxHeightJob.isExecutable()) {
            result.add(maxHeightJob);
        }
        if (minHeightJob.isExecutable()) {
            result.add(minHeightJob);
        }
        if (prefHeightJob.isExecutable()) {
            result.add(prefHeightJob);
        }
        return result;
    }

    private List<ModifyObjectJob> modifyWidthJobs(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final PropertyName maxWidth = new PropertyName("maxWidth");
        final PropertyName minWidth = new PropertyName("minWidth");
        final PropertyName prefWidth = new PropertyName("prefWidth");

        final ValuePropertyMetadata maxWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxWidth);
        final ValuePropertyMetadata minWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minWidth);
        final ValuePropertyMetadata prefWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefWidth);

        final ModifyObjectJob maxWidthJob = new ModifyObjectJob(
                candidate, maxWidthVPM, -1.0, getEditorController());
        final ModifyObjectJob minWidthJob = new ModifyObjectJob(
                candidate, minWidthVPM, -1.0, getEditorController());
        final ModifyObjectJob prefWidthJob = new ModifyObjectJob(
                candidate, prefWidthVPM, -1.0, getEditorController());

        if (maxWidthJob.isExecutable()) {
            result.add(maxWidthJob);
        }
        if (minWidthJob.isExecutable()) {
            result.add(minWidthJob);
        }
        if (prefWidthJob.isExecutable()) {
            result.add(prefWidthJob);
        }
        return result;
    }

    private List<ModifyObjectJob> modifyFitHeightJob(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final PropertyName fitHeight = new PropertyName("fitHeight");
        final ValuePropertyMetadata fitHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, fitHeight);
        final ModifyObjectJob fitHeightJob = new ModifyObjectJob(
                candidate, fitHeightVPM, 0.0, getEditorController());

        if (fitHeightJob.isExecutable()) {
            result.add(fitHeightJob);
        }
        return result;
    }

    private List<ModifyObjectJob> modifyFitWidthJob(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final PropertyName fitWidth = new PropertyName("fitWidth");
        final ValuePropertyMetadata fitWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, fitWidth);
        final ModifyObjectJob fitWidthJob = new ModifyObjectJob(
                candidate, fitWidthVPM, 0.0, getEditorController());

        if (fitWidthJob.isExecutable()) {
            result.add(fitWidthJob);
        }
        return result;
    }
}

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
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

/**
 * Job used to insert new FXOM objects into an accessory location.
 *
 */
public class InsertAsAccessoryJob extends Job {

    private final FXOMObject newObject;
    private final FXOMObject targetObject;
    private final Accessory accessory;
    private BatchJob subJob; // Initialized by execute()
    private String description; // final but initialized lazily

    public InsertAsAccessoryJob(
            FXOMObject newObject,
            FXOMObject targetObject,
            Accessory accessory,
            EditorController editorController) {
        super(editorController);

        assert newObject != null;
        assert targetObject != null;
        assert accessory != null;
        assert newObject.getFxomDocument() == getEditorController().getFxomDocument();
        assert targetObject.getFxomDocument() == getEditorController().getFxomDocument();

        this.newObject = newObject;
        this.targetObject = targetObject;
        this.accessory = accessory;
    }

    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        final boolean result;
        if (targetObject instanceof FXOMInstance) {
            final DesignHierarchyMask mask = new DesignHierarchyMask(targetObject);
            result = mask.isAcceptingAccessory(accessory, newObject)
                    && mask.getAccessory(accessory) == null;
        } else {
            // TODO(elp): someday we should support insering in FXOMCollection
            result = false;
        }
        return result;
    }

    @Override
    public void execute() {
        assert isExecutable(); // (1)

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final FXOMInstance targetInstance = (FXOMInstance) targetObject;
        final DesignHierarchyMask mask = new DesignHierarchyMask(targetObject);
        final PropertyName accessoryName = mask.getPropertyNameForAccessory(accessory);
        assert accessoryName != null;

        // Property has no value yet because of (1)
        FXOMProperty targetProperty = new FXOMPropertyC(fxomDocument, accessoryName);

        subJob = new BatchJob(getEditorController(),
                true /* shouldUpdateSceneGraph */, null);
        final Job addValueJob
                = new AddPropertyValueJob(newObject,
                        (FXOMPropertyC) targetProperty,
                        -1,
                        getEditorController());
        subJob.addSubJob(addValueJob);

        if (targetProperty.getParentInstance() == null) {
            assert targetObject instanceof FXOMInstance;
            final Job addPropertyJob
                    = new AddPropertyJob(targetProperty, targetInstance,
                            -1, getEditorController());
            subJob.addSubJob(addPropertyJob);
        }

        final Job pruneJob = new PrunePropertiesJob(newObject, targetObject, 
                getEditorController());
        if (pruneJob.isExecutable()) {
            subJob.prependSubJob(pruneJob);
        }
            
        /*
         * Executes the subjob.
         */
        subJob.execute();
    }

    @Override
    public void undo() {
        assert subJob != null;
        subJob.undo();
    }

    @Override
    public void redo() {
        assert subJob != null;
        subJob.redo();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            final StringBuilder sb = new StringBuilder();

            sb.append("Insert ");

            if (newObject instanceof FXOMInstance) {
                final Object sceneGraphObject = newObject.getSceneGraphObject();
                if (sceneGraphObject != null) {
                    sb.append(sceneGraphObject.getClass().getSimpleName());
                } else {
                    sb.append("Unresolved Object");
                }
            } else if (newObject instanceof FXOMCollection) {
                sb.append("Collection");
            } else {
                assert false;
                sb.append(newObject.getClass().getSimpleName());
            }
            description = sb.toString();
        }
        return description;
    }
}

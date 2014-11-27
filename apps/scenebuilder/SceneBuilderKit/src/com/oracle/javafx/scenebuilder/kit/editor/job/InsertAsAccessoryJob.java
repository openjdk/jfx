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
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job used to insert new FXOM objects into an accessory location.
 *
 */
public class InsertAsAccessoryJob extends BatchSelectionJob {

    private final FXOMObject newObject;
    private final FXOMObject targetObject;
    private final Accessory accessory;

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

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        if (targetObject instanceof FXOMInstance) {

            final DesignHierarchyMask mask = new DesignHierarchyMask(targetObject);
            if (mask.isAcceptingAccessory(accessory, newObject)
                    && mask.getAccessory(accessory) == null) { // (1)

                final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
                final FXOMInstance targetInstance = (FXOMInstance) targetObject;
                final PropertyName accessoryName = mask.getPropertyNameForAccessory(accessory);
                assert accessoryName != null;

                // Property has no value yet because of (1)
                FXOMProperty targetProperty = new FXOMPropertyC(fxomDocument, accessoryName);

                final Job addValueJob
                        = new AddPropertyValueJob(newObject,
                                (FXOMPropertyC) targetProperty,
                                -1,
                                getEditorController());
                result.add(addValueJob);

                if (targetProperty.getParentInstance() == null) {
                    assert targetObject instanceof FXOMInstance;
                    final Job addPropertyJob
                            = new AddPropertyJob(targetProperty, targetInstance,
                                    -1, getEditorController());
                    result.add(addPropertyJob);
                }

                final Job pruneJob = new PrunePropertiesJob(newObject, targetObject,
                        getEditorController());
                if (pruneJob.isExecutable()) {
                    result.add(0, pruneJob);
                }
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
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
        result = sb.toString();
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        final Set<FXOMObject> newObjects = new HashSet<>();
        newObjects.add(newObject);
        return new ObjectSelectionGroup(newObjects, newObject, null);
    }
}

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
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job used to insert new FXOM objects into a sub component location.
 *
 */
public class InsertAsSubComponentJob extends BatchSelectionJob {

    private final FXOMObject newObject;
    private final FXOMObject targetObject;
    private final int targetIndex;

    public InsertAsSubComponentJob(
            FXOMObject newObject,
            FXOMObject targetObject,
            int targetIndex,
            EditorController editorController) {
        super(editorController);

        assert newObject != null;
        assert targetObject != null;
        assert targetIndex >= -1;
        assert newObject.getFxomDocument() == getEditorController().getFxomDocument();
        assert targetObject.getFxomDocument() == getEditorController().getFxomDocument();

        this.newObject = newObject;
        this.targetObject = targetObject;
        this.targetIndex = targetIndex;
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result;
        
        final boolean executable;
        if (targetObject instanceof FXOMInstance) {
            final DesignHierarchyMask mask = new DesignHierarchyMask(targetObject);
            executable = mask.isAcceptingSubComponent(newObject);
        } else {
            // TODO(elp): someday we should support insering in FXOMCollection
            executable = false;
        }
        
        if (executable) {
            final FXOMDocument fxomDocument = targetObject.getFxomDocument();
            final FXOMInstance targetInstance = (FXOMInstance) targetObject;
            final DesignHierarchyMask mask = new DesignHierarchyMask(targetObject);
            final PropertyName subComponentName = mask.getSubComponentPropertyName();
            assert subComponentName != null;

            /*
             * Two cases:
             *  1) targetObject has no sub component yet
             *      => a new FXOMProperty must created
             *      => newObject must be added to this property using AddPropertyValueJob
             *      => new property must be added to targetObject using AddPropertyJob
             *  2) targetObject has already some sub components
             *      2.1) property is an FXOMPropertyC
             *          => newObject must be inserted amongst the existing values
             *      2.2) property is an empty FXOMPropertyT (see DTL-6206)
             *          => property must be replaced by an FXOMPropertyC
             *          => newObject must be inserted in the FXOMPropertyC
             */

            final FXOMProperty currentProperty
                    = targetInstance.getProperties().get(subComponentName);
           
            final FXOMPropertyC targetProperty;
            if (currentProperty instanceof FXOMPropertyC) {
                targetProperty = (FXOMPropertyC) currentProperty;
            } else {
                targetProperty = new FXOMPropertyC(fxomDocument, subComponentName);
            }
            
            result = new ArrayList<>();

            /*
             * RemovePropertyJob
             */
            if (currentProperty instanceof FXOMPropertyT) {
                result.add(new RemovePropertyJob(currentProperty, getEditorController()));
            }

            /*
             * AddPropertyValueJob
             */
            final Job addValueJob 
                    = new AddPropertyValueJob(newObject, 
                            targetProperty, 
                            targetIndex, 
                            getEditorController());
            result.add(addValueJob);

            /*
             * AddPropertyJob
             */
            if (targetProperty.getParentInstance() == null) {
                assert targetObject instanceof FXOMInstance;
                final Job addPropertyJob
                        = new AddPropertyJob(targetProperty, targetInstance,
                        -1, getEditorController());
                result.add(addPropertyJob);
            }
            
            /*
             * PrunePropertiesJob
             */
            final Job pruneJob = new PrunePropertiesJob(newObject, targetObject, 
                    getEditorController());
            if (pruneJob.isExecutable()) {
                result.add(0, pruneJob);
            }
            
        } else {
            result = Collections.emptyList();
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
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
        
        return sb.toString();
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        final Set<FXOMObject> newObjects = new HashSet<>();
        newObjects.add(newObject);
        return new ObjectSelectionGroup(newObjects, newObject, null);
    }
}

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
package com.oracle.javafx.scenebuilder.kit.editor.job.wrap;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyFxControllerJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.SetDocumentRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ToggleFxRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.RemovePropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Main class used for the unwrap jobs.
 */
public class UnwrapJob extends Job {

    private BatchJob batchJob;
    private AbstractSelectionGroup selectionSnapshot;
    private FXOMInstance oldContainer, newContainer;
    private List<FXOMObject> oldContainerChildren;

    public UnwrapJob(EditorController editorController) {
        super(editorController);
    }

    @Override
    public boolean isExecutable() {
        return WrapJobUtils.canUnwrap(getEditorController());
    }

    @Override
    public void execute() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        assert isExecutable();
        buildSubJobs();

        try {
            selectionSnapshot = selection.getGroup().clone();
        } catch (CloneNotSupportedException x) {
            // Emergency code
            throw new RuntimeException(x);
        }
        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        batchJob.execute();
        fxomDocument.endUpdate();
        selection.select(oldContainerChildren);
        selection.endUpdate();
    }

    @Override
    public void undo() {
        assert batchJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.beginUpdate();
        fxomDocument.beginUpdate();
        batchJob.undo();
        fxomDocument.endUpdate();
        selection.select(selectionSnapshot);
        selection.endUpdate();
    }

    @Override
    public void redo() {
        assert batchJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        batchJob.redo();
        fxomDocument.endUpdate();
        selection.select(oldContainerChildren);
        selection.endUpdate();
    }

    @Override
    public String getDescription() {
        return "Unwrap";
    }

    protected void buildSubJobs() {

        assert isExecutable(); // (1)

        // Create batch job
        batchJob = new BatchJob(getEditorController());

        final Selection selection = getEditorController().getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();
        assert asg instanceof ObjectSelectionGroup; // Because of (1)
        final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
        assert osg.getItems().size() == 1; // Because of (1)

        // Retrieve the old container (container to unwrap)
        oldContainer = (FXOMInstance) osg.getItems().iterator().next();
        // Retrieve the children of the old container
        oldContainerChildren = getChildren(oldContainer);
        // Retrieve the old container property name in use
        final PropertyName oldContainerPropertyName
                = WrapJobUtils.getContainerPropertyName(oldContainer, oldContainerChildren);
        // Retrieve the old container property (already defined and not null)
        final FXOMPropertyC oldContainerProperty
                = (FXOMPropertyC) oldContainer.getProperties().get(oldContainerPropertyName);
        assert oldContainerProperty != null
                && oldContainerProperty.getParentInstance() != null;

        // Retrieve the parent of the old container (aka new container)
        newContainer = (FXOMInstance) oldContainer.getParentObject();

        // Remove the old container property from the old container instance
        final Job removePropertyJob = new RemovePropertyJob(
                oldContainerProperty,
                getEditorController());
        batchJob.addSubJob(removePropertyJob);

        // Remove the children from the old container property
        final List<Job> removeChildrenJobs
                = removeChildrenFromPropertyJobs(oldContainerProperty, oldContainerChildren);
        batchJob.addSubJobs(removeChildrenJobs);

        //------------------------------------------------------------------
        // If the target object is NOT the FXOM root :
        // - we update the new container bounds and add it to the current container
        // - we update the children bounds and remove them from the current container
        //------------------------------------------------------------------
        if (newContainer != null) {

            // Retrieve the new container property name in use
            final List<FXOMObject> newContainerChildren = new ArrayList<>();
            newContainerChildren.add(oldContainer);
            final PropertyName newContainerPropertyName
                    = WrapJobUtils.getContainerPropertyName(newContainer, newContainerChildren);
            // Retrieve the new container property (already defined and not null)
            final FXOMPropertyC newContainerProperty
                    = (FXOMPropertyC) newContainer.getProperties().get(newContainerPropertyName);
            assert newContainerProperty != null
                    && newContainerProperty.getParentInstance() != null;

            // Update children bounds before adding them to the new container
            final DesignHierarchyMask newContainerMask
                    = new DesignHierarchyMask(newContainer);
            if (newContainerMask.isFreeChildPositioning()) {
                final List<Job> modifyChildrenLayoutJobs
                        = modifyChildrenLayoutJobs(oldContainerChildren);
                batchJob.addSubJobs(modifyChildrenLayoutJobs);
            }

            // Add the children to the new container
            int index = oldContainer.getIndexInParentProperty();
            final List<Job> addChildrenJobs
                    = addChildrenToPropertyJobs(newContainerProperty, index, oldContainerChildren);
            batchJob.addSubJobs(addChildrenJobs);

            // Remove the old container from the new container property
            final Job removeValueJob = new RemovePropertyValueJob(
                    oldContainer,
                    getEditorController());
            batchJob.addSubJob(removeValueJob);
        } //
        //------------------------------------------------------------------
        // If the target object is the FXOM root :
        // - we update the document root with the single child of the root node
        //------------------------------------------------------------------
        else {
            assert oldContainerChildren.size() == 1; // Because of (1)
            boolean isFxRoot = oldContainer.isFxRoot();
            final String fxController = oldContainer.getFxController();
            // First remove the fx:controller/fx:root from the old root object
            if (isFxRoot) {
                final ToggleFxRootJob fxRootJob = new ToggleFxRootJob(getEditorController());
                batchJob.addSubJob(fxRootJob);
            }
            if (fxController != null) {
                final ModifyFxControllerJob fxControllerJob
                        = new ModifyFxControllerJob(oldContainer, null, getEditorController());
                batchJob.addSubJob(fxControllerJob);
            }
            // Then set the new container as root object            
            final FXOMObject child = oldContainerChildren.iterator().next();
            final Job setDocumentRoot = new SetDocumentRootJob(
                    child, getEditorController());
            batchJob.addSubJob(setDocumentRoot);
            // Finally add the fx:controller/fx:root to the new root object
            if (isFxRoot) {
                final ToggleFxRootJob fxRootJob = new ToggleFxRootJob(getEditorController());
                batchJob.addSubJob(fxRootJob);
            }
            if (fxController != null) {
                final ModifyFxControllerJob fxControllerJob
                        = new ModifyFxControllerJob(child, fxController, getEditorController());
                batchJob.addSubJob(fxControllerJob);
            }
        }
    }

    protected List<Job> addChildrenToPropertyJobs(
            final FXOMPropertyC containerProperty,
            final int start,
            final List<FXOMObject> children) {

        final List<Job> jobs = new ArrayList<>();
        int index = start;
        for (FXOMObject child : children) {
            assert child instanceof FXOMInstance;
            final Job addValueJob = new AddPropertyValueJob(
                    child,
                    containerProperty,
                    index++,
                    getEditorController());
            jobs.add(addValueJob);
        }
        return jobs;
    }

    protected List<Job> removeChildrenFromPropertyJobs(
            final FXOMPropertyC containerProperty,
            final List<FXOMObject> children) {

        final List<Job> jobs = new ArrayList<>();
        for (FXOMObject child : children) {
            assert child instanceof FXOMInstance;
            final Job removeValueJob = new RemovePropertyValueJob(
                    child,
                    getEditorController());
            jobs.add(removeValueJob);
        }
        return jobs;
    }

    /**
     * Used to modify the children layout properties.
     *
     * @param children The children.
     * @return
     */
    protected List<Job> modifyChildrenLayoutJobs(final List<FXOMObject> children) {

        final List<Job> jobs = new ArrayList<>();

        assert oldContainer.getSceneGraphObject() instanceof Node;
        final Node oldContainerNode = (Node) oldContainer.getSceneGraphObject();
        final Bounds oldContainerBounds = oldContainerNode.getLayoutBounds();
        final Point2D point = oldContainerNode.localToParent(
                oldContainerBounds.getMinX(), oldContainerBounds.getMinY());

        for (FXOMObject child : children) {
            assert child.getSceneGraphObject() instanceof Node;
            final Node childNode = (Node) child.getSceneGraphObject();
            double layoutX = point.getX() + childNode.getLayoutX();
            double layoutY = point.getY() + childNode.getLayoutY();
            final ModifyObjectJob modifyLayoutX = WrapJobUtils.modifyObjectJob(
                    (FXOMInstance) child, "layoutX", layoutX, getEditorController());
            jobs.add(modifyLayoutX);
            final ModifyObjectJob modifyLayoutY = WrapJobUtils.modifyObjectJob(
                    (FXOMInstance) child, "layoutY", layoutY, getEditorController());
            jobs.add(modifyLayoutY);
        }
        return jobs;
    }

    private List<FXOMObject> getChildren(final FXOMObject container) {
        final DesignHierarchyMask mask = new DesignHierarchyMask(container);
        final List<FXOMObject> result = new ArrayList<>();
        if (mask.isAcceptingSubComponent()) {
            for (int i = 0, count = mask.getSubComponentCount(); i < count; i++) {
                final FXOMObject child = mask.getSubComponentAtIndex(i);
                result.add(child);
            }
        } else {
            assert mask.isAcceptingAccessory(Accessory.CONTENT);
            final FXOMObject child = mask.getAccessory(Accessory.CONTENT);
            result.add(child);
        }
        return result;
    }
}

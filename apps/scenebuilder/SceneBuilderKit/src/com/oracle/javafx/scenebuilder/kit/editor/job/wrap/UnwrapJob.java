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
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyFxControllerJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.SetDocumentRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ToggleFxRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemovePropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
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
import javafx.scene.control.DialogPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;

/**
 * Main class used for the unwrap jobs.
 */
public class UnwrapJob extends BatchSelectionJob {

    private FXOMInstance oldContainer, newContainer;
    private List<FXOMObject> oldContainerChildren;

    public UnwrapJob(EditorController editorController) {
        super(editorController);
    }

    protected boolean canUnwrap() {
        final Selection selection = getEditorController().getSelection();
        if (selection.isEmpty()) {
            return false;
        }
        final AbstractSelectionGroup asg = selection.getGroup();
        if ((asg instanceof ObjectSelectionGroup) == false) {
            return false;
        }
        final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
        if (osg.getItems().size() != 1) {
            return false;
        }
        final FXOMObject container = osg.getItems().iterator().next();
        if (container instanceof FXOMInstance == false) {
            return false;
        }
        final FXOMInstance containerInstance = (FXOMInstance) container;
        
        // Unresolved custom type
        if (container.getSceneGraphObject() == null) {
            return false;
        }
        
        // Cannot unwrap TabPane
        if (TabPane.class.isAssignableFrom(containerInstance.getDeclaredClass())) {
            return false;
        }
        // Cannot unwrap all Pane subclasses (ex : BorderPane, TextFlow and DialogPane)
        if (BorderPane.class.isAssignableFrom(containerInstance.getDeclaredClass())
                || TextFlow.class.isAssignableFrom(containerInstance.getDeclaredClass())
                || DialogPane.class.isAssignableFrom(containerInstance.getDeclaredClass())) {
            return false;
        }
        // Can unwrap classes supporting wrapping except TabPane + some Pane subclasses (see above)
        boolean isAssignableFrom = false;
        for (Class<?> clazz : EditorController.getClassesSupportingWrapping()) {
            isAssignableFrom |= clazz.isAssignableFrom(
                    containerInstance.getDeclaredClass());
        }
        if (isAssignableFrom == false) {
            return false;
        }

        // Retrieve the num of children of the container to unwrap
        int childrenCount = getChildren(container).size();
        // If the container to unwrap has no childen, it cannot be unwrapped
        if (childrenCount == 0) {
            return false;
        }

        // Retrieve the parent of the container to unwrap
        final FXOMObject parentContainer = container.getParentObject();
        // Unwrap the root node
        if (parentContainer == null) {
            return childrenCount == 1;
        } else {
            // Check that the num of children can be added to the parent container
            final DesignHierarchyMask parentContainerMask
                    = new DesignHierarchyMask(parentContainer);
            if (parentContainerMask.isAcceptingSubComponent()) {
                return childrenCount >= 1;
            } else {
                assert parentContainerMask.isAcceptingAccessory(Accessory.CONTENT)
                        || parentContainerMask.isAcceptingAccessory(Accessory.GRAPHIC)
                        || parentContainerMask.getFxomObject().getSceneGraphObject() instanceof BorderPane
                        || parentContainerMask.getFxomObject().getSceneGraphObject() instanceof DialogPane;
                return childrenCount == 1;
            }
        }
    }
    
    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();

        if (canUnwrap()) { // (1)

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
            result.add(removePropertyJob);

            // Remove the children from the old container property
            final List<Job> removeChildrenJobs
                    = removeChildrenJobs(oldContainerProperty, oldContainerChildren);
            result.addAll(removeChildrenJobs);

            //------------------------------------------------------------------
            // If the target object is NOT the FXOM root :
            // - we update the new container bounds and add it to the old container
            // - we update the children bounds and remove them from the old container
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
                            = modifyChildrenJobs(oldContainerChildren);
                    result.addAll(modifyChildrenLayoutJobs);
                }

                // Add the children to the new container
                int index = oldContainer.getIndexInParentProperty();
                final List<Job> addChildrenJobs
                        = addChildrenJobs(newContainerProperty, index, oldContainerChildren);
                result.addAll(addChildrenJobs);

                // Remove the old container from the new container property
                final Job removeValueJob = new RemovePropertyValueJob(
                        oldContainer,
                        getEditorController());
                result.add(removeValueJob);
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
                    result.add(fxRootJob);
                }
                if (fxController != null) {
                    final ModifyFxControllerJob fxControllerJob
                            = new ModifyFxControllerJob(oldContainer, null, getEditorController());
                    result.add(fxControllerJob);
                }
                // Then set the new container as root object            
                final FXOMObject child = oldContainerChildren.iterator().next();
                final Job setDocumentRoot = new SetDocumentRootJob(
                        child, getEditorController());
                result.add(setDocumentRoot);
                // Finally add the fx:controller/fx:root to the new root object
                if (isFxRoot) {
                    final ToggleFxRootJob fxRootJob = new ToggleFxRootJob(getEditorController());
                    result.add(fxRootJob);
                }
                if (fxController != null) {
                    final ModifyFxControllerJob fxControllerJob
                            = new ModifyFxControllerJob(child, fxController, getEditorController());
                    result.add(fxControllerJob);
                }
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        return "Unwrap";
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        return new ObjectSelectionGroup(oldContainerChildren, oldContainerChildren.iterator().next(), null);
    }

    protected List<Job> addChildrenJobs(
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

    protected List<Job> removeChildrenJobs(
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
     * Used to modify the specified children.
     *
     * @param children The children.
     * @return A list of jobs.
     */
    protected List<Job> modifyChildrenJobs(final List<FXOMObject> children) {

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
            result.addAll(mask.getSubComponents());
        } else {
            assert mask.isAcceptingAccessory(Accessory.CONTENT);
            final FXOMObject child = mask.getAccessory(Accessory.CONTENT);
            if (child != null) {
                result.add(child);
            }
        }
        return result;
    }
}

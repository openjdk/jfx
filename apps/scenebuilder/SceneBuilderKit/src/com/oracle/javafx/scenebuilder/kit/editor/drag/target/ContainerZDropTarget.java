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
package com.oracle.javafx.scenebuilder.kit.editor.drag.target;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.DeleteObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.InsertAsSubComponentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ReIndexObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.AdjustAllToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class ContainerZDropTarget extends AbstractDropTarget {

    private final FXOMInstance targetContainer;
    private final FXOMObject beforeChild;

    public ContainerZDropTarget(FXOMInstance targetContainer, FXOMObject beforeChild) {
        assert targetContainer != null;
        this.targetContainer = targetContainer;
        this.beforeChild = beforeChild;
    }

    public FXOMObject getBeforeChild() {
        return beforeChild;
    }
    

    /*
     * AbstractDropTarget
     */
    @Override
    public FXOMObject getTargetObject() {
        return targetContainer;
    }

    @Override
    public boolean acceptDragSource(AbstractDragSource dragSource) {
        assert dragSource != null;
        
        final boolean result;
        if (dragSource.getDraggedObjects().isEmpty()) {
            result = false;
        } else {
            final DesignHierarchyMask m = new DesignHierarchyMask(targetContainer);
            if (m.isAcceptingSubComponent(dragSource.getDraggedObjects())) {
                final FXOMObject draggedObject0 = dragSource.getDraggedObjects().get(0);
                final boolean sameContainer 
                        = targetContainer == draggedObject0.getParentObject();
                final boolean sameIndex 
                        = (beforeChild == draggedObject0) 
                        || (beforeChild == draggedObject0.getNextSlibing());
                        
                result = (sameContainer == false) || (sameIndex == false);
            } else {
                result = false;
            }
        }
        
        return result;
    }

    @Override
    public Job makeDropJob(AbstractDragSource dragSource, EditorController editorController) {
        assert dragSource != null;
        assert dragSource.getDraggedObjects().isEmpty() == false;
        assert editorController != null;
        
        final boolean shouldRefreshSceneGraph = true;
        final BatchJob result = new BatchJob(editorController,
                shouldRefreshSceneGraph, dragSource.makeDropJobDescription());
        
        final List<FXOMObject> draggedObjects = dragSource.getDraggedObjects();
        final FXOMObject currentParent = draggedObjects.get(0).getParentObject();
        
        if (currentParent == targetContainer) {
            // It's a re-indexing job
            for (FXOMObject draggedObject : dragSource.getDraggedObjects()) {
                result.addSubJob(new ReIndexObjectJob(
                        draggedObject, beforeChild, editorController));
            }
        } else {
            // It's a reparening job :
            //  - remove drag source objects from their current parent (if any)
            //  - add drag source objects to this drop target
            
            if (currentParent != null) {
                for (FXOMObject draggedObject : draggedObjects) {
                    result.addSubJob(new DeleteObjectJob(draggedObject,
                            editorController));
                }
            }
            int targetIndex;
            if (beforeChild == null) {
                final DesignHierarchyMask m = new DesignHierarchyMask(targetContainer);
                targetIndex = m.getSubComponentCount();
            } else {
                targetIndex = beforeChild.getIndexInParentProperty();
                assert targetIndex != -1;
            }
            for (FXOMObject draggedObject : draggedObjects) {
                final Job j = new InsertAsSubComponentJob(draggedObject, 
                        targetContainer, targetIndex++, editorController);
                result.addSubJob(j);
            }
        }
            
        result.addSubJob(new AdjustAllToggleGroupJob(editorController));
        
        assert result.isExecutable();
        
        return result;
    }
    
    @Override
    public boolean isSelectRequiredAfterDrop() {
        return true;
    }
    
    /*
     * Objects
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.targetContainer);
        hash = 97 * hash + Objects.hashCode(this.beforeChild);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContainerZDropTarget other = (ContainerZDropTarget) obj;
        if (!Objects.equals(this.targetContainer, other.targetContainer)) {
            return false;
        }
        if (!Objects.equals(this.beforeChild, other.beforeChild)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ContainerZDropTarget{" + "targetContainer=" + targetContainer + ", beforeChild=" + beforeChild + '}'; //NOI18N
    }
    
    
}

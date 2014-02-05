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
import com.oracle.javafx.scenebuilder.kit.editor.job.InsertAsAccessoryJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class AccessoryDropTarget extends AbstractDropTarget {

    private final FXOMInstance targetContainer;
    private final Accessory accessory;

    public AccessoryDropTarget(FXOMInstance targetContainer, Accessory accessory) {
        assert targetContainer != null;
        this.targetContainer = targetContainer;
        this.accessory = accessory;
    }

    public Accessory getAccessory() {
        return accessory;
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
        if (dragSource.getDraggedObjects().size() != 1) {
            result = false;
        } else {
            final DesignHierarchyMask m = new DesignHierarchyMask(targetContainer);
            final FXOMObject draggedObject = dragSource.getDraggedObjects().get(0);
            result = m.isAcceptingAccessory(accessory, draggedObject)
                    && m.getAccessory(accessory) == null;
        }
        
        return result;
    }

    @Override
    public Job makeDropJob(AbstractDragSource dragSource, EditorController editorController) {
        assert acceptDragSource(dragSource);
        assert editorController != null;
        
        final boolean shouldRefreshSceneGraph = true;
        final BatchJob result = new BatchJob(editorController,
                shouldRefreshSceneGraph, dragSource.makeDropJobDescription());
        
        final FXOMObject draggedObject = dragSource.getDraggedObjects().get(0);
        final FXOMObject currentParent = draggedObject.getParentObject();
        
        // Two steps :
        //  - remove drag source object from its current parent (if any)
        //  - set the drag source object as accessory of the drop target

        if (currentParent != null) {
            result.addSubJob(new DeleteObjectJob(draggedObject, editorController));
        }
        final Job j = new InsertAsAccessoryJob(draggedObject, 
                targetContainer, accessory, editorController);
        result.addSubJob(j);
        
        if (targetContainer.getSceneGraphObject() instanceof BorderPane) {
            assert draggedObject instanceof FXOMInstance;
            
            // We add a job which sets BorderPane.alignment=CENTER on draggedObject
            final FXOMInstance draggedInstance
                    = (FXOMInstance) draggedObject;
            final PropertyName alignmentName
                    = new PropertyName("alignment", BorderPane.class); //NOI18N
            final EnumerationPropertyMetadata alignmentMeta
                    = new EnumerationPropertyMetadata(alignmentName, Pos.class,
                    "UNUSED", true /* readWrite */, InspectorPath.UNUSED); //NOI18N
            final Job alignmentJob
                    = new ModifyObjectJob(draggedInstance, alignmentMeta, 
                            Pos.CENTER.toString(), editorController);
            result.addSubJob(alignmentJob);
        }
        
        assert result.isExecutable();
        
        return result;
    }
    
    /*
     * Objects
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.targetContainer);
        hash = 97 * hash + (this.accessory != null ? this.accessory.hashCode() : 0);
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
        final AccessoryDropTarget other = (AccessoryDropTarget) obj;
        if (!Objects.equals(this.targetContainer, other.targetContainer)) {
            return false;
        }
        if (this.accessory != other.accessory) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AccessoryDropTarget{" + "targetContainer=" + targetContainer + ", accessory=" + accessory + '}'; //NOI18N
    }
    
    
}

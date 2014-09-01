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
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.DeleteObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.InsertAsSubComponentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RelocateNodeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.AdjustAllToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 *
 */
public class ContainerXYDropTarget extends AbstractDropTarget {

    private final FXOMInstance targetContainer;
    private final double sceneX;
    private final double sceneY;

    public ContainerXYDropTarget(FXOMInstance targetContainer, double sceneX, double sceneY) {
        assert targetContainer != null;
        assert targetContainer.getSceneGraphObject() instanceof Parent;
        this.targetContainer = targetContainer;
        this.sceneX = sceneX;
        this.sceneY = sceneY;
    }

    public double getSceneX() {
        return sceneX;
    }

    public double getSceneY() {
        return sceneY;
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
            boolean containsIntrinsic = false;
            for (FXOMObject draggedObject : dragSource.getDraggedObjects()) {
                if (draggedObject instanceof FXOMIntrinsic) {
                    containsIntrinsic = true;
                }
            }
            if (containsIntrinsic) {
                result = false;
            } else {
                final DesignHierarchyMask m = new DesignHierarchyMask(targetContainer);
                result = m.isAcceptingSubComponent(dragSource.getDraggedObjects());
            }
        }
        
        return result;
    }

    @Override
    public Job makeDropJob(AbstractDragSource dragSource, EditorController editorController) {
        assert acceptDragSource(dragSource);
        assert editorController != null;
        
        
        final List<FXOMObject> draggedObjects = dragSource.getDraggedObjects();
        final FXOMObject hitObject = dragSource.getHitObject();
        final double hitX = dragSource.getHitX();
        final double hitY = dragSource.getHitY();
        final FXOMObject currentParent = hitObject.getParentObject();
        
        final BatchJob result;
        if (currentParent == targetContainer) {
            // It's a relocating job
            assert hitObject.getSceneGraphObject() instanceof Node;
            assert hitObject instanceof FXOMInstance;
            
            final boolean shouldRefreshSceneGraph = false;
            result = new BatchJob(editorController, 
                    shouldRefreshSceneGraph, dragSource.makeDropJobDescription());
            
            final Point2D dxy = computeRelocationDXY((FXOMInstance) hitObject, hitX, hitY);
            for (FXOMObject draggedObject : dragSource.getDraggedObjects()) {
                assert draggedObject.getSceneGraphObject() instanceof Node;
                assert draggedObject instanceof FXOMInstance;
                final Node draggedNode = (Node) draggedObject.getSceneGraphObject();
                final double newLayoutX = Math.round(draggedNode.getLayoutX() + dxy.getX());
                final double newLayoutY = Math.round(draggedNode.getLayoutY() + dxy.getY());
                result.addSubJob(new RelocateNodeJob((FXOMInstance)draggedObject, 
                        newLayoutX, newLayoutY, editorController));
            }
        } else {
            // It's a reparening job :
            //  - remove drag source objects from their current parent (if any)
            //  - add drag source objects to this drop target
            //  - relocate the drag source objects
            //  - adjust toggle group declaration (if any)
            
            final boolean shouldRefreshSceneGraph = true;
            result = new BatchJob(editorController, 
                    shouldRefreshSceneGraph, dragSource.makeDropJobDescription());
            
            if (currentParent != null) {
                for (FXOMObject draggedObject : draggedObjects) {
                    result.addSubJob(new DeleteObjectJob(draggedObject,
                            editorController));
                }
            }
            for (FXOMObject draggedObject : draggedObjects) {
                result.addSubJob(new InsertAsSubComponentJob(
                        draggedObject, targetContainer, -1, editorController));
            }
            
            // Computes dragged object positions relatively to hitObject
            assert hitObject.getSceneGraphObject() instanceof Node;
            final Node hitNode = (Node) hitObject.getSceneGraphObject();
            final double layoutX0 = hitNode.getLayoutX();
            final double layoutY0 = hitNode.getLayoutY();
            final Map<FXOMObject, Point2D> layoutDXY = new HashMap<>();
            for (FXOMObject draggedObject : draggedObjects) {
                assert draggedObject.getSceneGraphObject() instanceof Node;
                final Node draggedNode = (Node) draggedObject.getSceneGraphObject();
                final double layoutDX = draggedNode.getLayoutX() - layoutX0;
                final double layoutDY = draggedNode.getLayoutY() - layoutY0;
                layoutDXY.put(draggedObject, new Point2D(layoutDX, layoutDY));
            }
            
            final Parent targetParent = (Parent)targetContainer.getSceneGraphObject();
            final Point2D targetCenter = targetParent.sceneToLocal(sceneX, sceneY);
            final Bounds layoutBounds = hitNode.getLayoutBounds();
            final Point2D currentOrigin = hitNode.localToParent(0.0, 0.0);
            final Point2D currentCenter = hitNode.localToParent(
                    (layoutBounds.getMinX() + layoutBounds.getMaxX()) / 2.0,
                    (layoutBounds.getMinY() + layoutBounds.getMaxY()) / 2.0);
            final double currentDX = currentOrigin.getX() - currentCenter.getX();
            final double currentDY = currentOrigin.getY() - currentCenter.getY();
            final double targetOriginX = targetCenter.getX() + currentDX;
            final double targetOriginY = targetCenter.getY() + currentDY;
            
            for (FXOMObject draggedObject : draggedObjects) {
                assert draggedObject instanceof FXOMInstance;
                final Point2D dxy = layoutDXY.get(draggedObject);
                assert dxy != null;
                final double newLayoutX = Math.round(targetOriginX + dxy.getX());
                final double newLayoutY = Math.round(targetOriginY + dxy.getY());
                result.addSubJob(new RelocateNodeJob((FXOMInstance)draggedObject, 
                        newLayoutX, newLayoutY, editorController));
            }
            
            result.addSubJob(new AdjustAllToggleGroupJob(editorController));
        }
        
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
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.targetContainer);
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.sceneX) ^ (Double.doubleToLongBits(this.sceneX) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.sceneY) ^ (Double.doubleToLongBits(this.sceneY) >>> 32));
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
        final ContainerXYDropTarget other = (ContainerXYDropTarget) obj;
        if (!Objects.equals(this.targetContainer, other.targetContainer)) {
            return false;
        }
        if (Double.doubleToLongBits(this.sceneX) != Double.doubleToLongBits(other.sceneX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.sceneY) != Double.doubleToLongBits(other.sceneY)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ContainerXYDropTarget{" + "targetContainer=" + targetContainer + ", sceneX=" + sceneX + ", sceneY=" + sceneY + '}'; //NOI18N
    }
    
    
    /*
     * Private
     */
    
    private Point2D computeRelocationDXY(FXOMInstance hitObject, double hitX, double hitY) {
        assert hitObject != null;
        assert hitObject.getSceneGraphObject() instanceof Node;
        
        /*
         * Converts (hitX, hitY) in hitObject parent coordinate space.
         */
        final Node sceneGraphNode = (Node)hitObject.getSceneGraphObject();
        final Point2D currentHit = sceneGraphNode.localToParent(hitX, hitY);
        
        /*
         * Computes drop target location in hitObject parent coordinate space
         */
        final Parent sceneGraphParent = sceneGraphNode.getParent();
        final Point2D newHit = sceneGraphParent.sceneToLocal(sceneX, sceneY);
        
        final double dx = newHit.getX() - currentHit.getX();
        final double dy = newHit.getY() - currentHit.getY();
        return new Point2D(dx, dy);
    }
}

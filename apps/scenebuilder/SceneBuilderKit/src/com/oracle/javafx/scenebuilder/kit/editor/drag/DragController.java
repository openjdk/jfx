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
package com.oracle.javafx.scenebuilder.kit.editor.drag;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.TransferMode;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.DocumentDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.BackupSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyPath;

/**
 *
 */
public class DragController {
    
    private final EditorController editorController;
    private final ObjectProperty<AbstractDragSource> dragSourceProperty
            = new SimpleObjectProperty<>(null);
    private final ObjectProperty<AbstractDropTarget> dropTargetProperty
            = new SimpleObjectProperty<>(null);
    private LiveUpdater liveUpdater;
    private Job backupSelectionJob;
    private boolean liveUpdateEnabled;
    private boolean dropAccepted;
    private AbstractDropTarget committedDropTarget;
    private Timer mouseTimer;
    
    public DragController(EditorController editorController) {
        this.editorController = editorController;
    }
    
    public void begin(AbstractDragSource dragSource) {
        assert dragSource != null;
        assert dragSource.isAcceptable();
        assert getDragSource() == null;
        assert getDropTarget() == null;
        assert liveUpdater == null;
        assert backupSelectionJob == null;
        assert dropAccepted == false;
        assert committedDropTarget == null;
        assert mouseTimer == null;
        
        liveUpdater = new LiveUpdater(dragSource, editorController);
        dragSourceProperty.set(dragSource);
        dropTargetProperty.set(null);
        
        // Backup and clear the selection
        backupSelectionJob = new BackupSelectionJob(editorController);
        editorController.getSelection().clear();
    }
    
    public void end() {
        assert getDragSource() != null;
        
        liveUpdater.setDropTarget(null);
        
        /*
         * Note 1: we reset the drop target before performing the drop operation.
         * This makes content panel hide the drop target ring before fxom update
         * and scene graph refresh.
         * Note 2: dropAccepted is reset before dropTargetProperty
         * so that listeners can invoke isDropAccepted().
         */
        dropAccepted = false;
        dropTargetProperty.set(null);

        if (committedDropTarget != null) {
            assert committedDropTarget.acceptDragSource(getDragSource());
            final Job dropJob 
                    = committedDropTarget.makeDropJob(getDragSource(), editorController);
            final Job selectJob 
                    = new UpdateSelectionJob(getDragSource().getDraggedObjects(), editorController);
            final BatchJob batchJob 
                    = new BatchJob(editorController, dropJob.getDescription());
            if (committedDropTarget.isSelectRequiredAfterDrop()) {
                batchJob.addSubJob(backupSelectionJob);
            }
            batchJob.addSubJob(dropJob);
            if (committedDropTarget.isSelectRequiredAfterDrop()) {
                batchJob.addSubJob(selectJob);
            }
            editorController.getJobManager().push(batchJob);
        }
        
        if (mouseTimer != null) {
            mouseTimer.cancel();
            mouseTimer = null;
        }
        liveUpdater = null;
        backupSelectionJob = null;
        committedDropTarget = null;
        dragSourceProperty.set(null);
        
    }

    public AbstractDragSource getDragSource() {
        return dragSourceProperty.get();
    }
    
    public Property<AbstractDragSource> dragSourceProperty() {
        return dragSourceProperty;
    }
    
    public void setDropTarget(AbstractDropTarget newDropTarget) {
        assert getDragSource() != null;
        assert (newDropTarget == null) || (this.committedDropTarget == null);
        
        /*
         * Update drop target property.
         * Note that this.dropAccepted is updated before so that
         * drop target listeners can invoke isDropAccepted().
         */
        if (newDropTarget == null) {
            dropAccepted = false;
        } else if (isDragSourceInParentChain(newDropTarget)) {
            dropAccepted = false;
        } else {
            dropAccepted = newDropTarget.acceptDragSource(getDragSource());
        }
        dropTargetProperty.set(newDropTarget);

        trackMouse();
        
        if (dropAccepted) {
            assert getDropTarget() != null;
            assert getDropTarget().acceptDragSource(getDragSource());
            assert getDragSource().getDraggedObjects().isEmpty() == false;
            
            final FXOMObject firstObject = getDragSource().getDraggedObjects().get(0);
            final FXOMObject currentParent = firstObject.getParentObject();
            final FXOMObject nextParent = getDropTarget().getTargetObject();
            
            if ((currentParent == nextParent) && liveUpdateEnabled) {
                liveUpdater.setDropTarget(newDropTarget);
            }
        }
    }
    
    public AbstractDropTarget getDropTarget() {
        return dropTargetProperty.get();
    }
    
    public Property<AbstractDropTarget> dropTargetProperty() {
        return dropTargetProperty;
    }
    
    public boolean isDropAccepted() {
        return dropAccepted;
    }
    
    public TransferMode[] getAcceptedTransferModes() {
        final TransferMode[] result;
        
        if (getDropTarget() == null) {
            result = TransferMode.NONE;
        } else if (dropAccepted) {
            if (getDragSource() instanceof DocumentDragSource) {
                result = new TransferMode[] { TransferMode.MOVE };
            } else  {
                result = new TransferMode[] { TransferMode.COPY };
            }
        } else {
            result = TransferMode.NONE;
        }
        
        assert (result.length == 0) || (getDropTarget() != null);
        
        return result;
    }
    
    public void commit() {
        assert isDropAccepted();
        assert committedDropTarget == null;
        
        committedDropTarget = getDropTarget();
    }
    
    public boolean isLiveUpdated() {
        return getDropTarget() == liveUpdater.getDropTarget();
    }
    
    /*
     * Private
     */
    
    private void mouseDidStopMoving() {
        if (dropAccepted 
                && (getDropTarget() != liveUpdater.getDropTarget()) 
                && liveUpdateEnabled) {
            liveUpdater.setDropTarget(getDropTarget());
        }
    }
    
    private static final long MOUSE_TIMER_DELAY = 500; // ms
    private void trackMouse() {
        final boolean runAsDaemon = true;
        
        if (mouseTimer == null) {
            mouseTimer = new Timer(runAsDaemon);
        } else {
            mouseTimer.cancel();
            mouseTimer = new Timer(runAsDaemon);
        }
        
        mouseTimer.schedule(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    mouseTimer = null;
                    mouseDidStopMoving();
                });
            }
        }, MOUSE_TIMER_DELAY);
    }
    
    /**
     * Returns true if one of the dragged object is in the parent chain of the
     * specified drop target, false otherwise.
     *
     * @param newDropTarget the drop target
     * @return true if one of the dragged object is in the parent chain of the
     * specified drop target, false otherwise
     */
    private boolean isDragSourceInParentChain(AbstractDropTarget newDropTarget) {
        assert newDropTarget != null;
        boolean result;
        
        if (newDropTarget instanceof RootDropTarget) {
            // dragSource is dragged over an empty document
            result = false;
        } else {
            final List<FXOMObject> draggedObjects
                    = getDragSource().getDraggedObjects();
            final DesignHierarchyPath dropTargetPath
                    = new DesignHierarchyPath(newDropTarget.getTargetObject());
            
            result = false;
            for (FXOMObject draggedObject : draggedObjects) {
                final DesignHierarchyPath draggedObjectPath
                        = new DesignHierarchyPath(draggedObject);
                final DesignHierarchyPath commonPath
                        = draggedObjectPath.getCommonPathWith(dropTargetPath);
                // If one of the dragged objects is in the parent chain 
                // of the drop target, we abort the DND gesture
                if (commonPath.equals(draggedObjectPath)) {
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
}

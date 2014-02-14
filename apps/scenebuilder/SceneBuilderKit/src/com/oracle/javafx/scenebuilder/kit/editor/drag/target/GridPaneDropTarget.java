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
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2.GridSnapshot;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2.InsertColumnJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2.InsertRowJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2.MoveCellContentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.ClearSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.util.GridBounds;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/**
 *
 */
public class GridPaneDropTarget extends AbstractDropTarget {
    
    public enum ColumnArea {
        LEFT, CENTER, RIGHT
    }
    
    public enum RowArea {
        TOP, CENTER, BOTTOM
    }

    private final FXOMObject targetGridPane;
    private final int targetColumnIndex;
    private final int targetRowIndex;
    private final ColumnArea targetColumnArea;
    private final RowArea targetRowArea;

    public GridPaneDropTarget(FXOMObject targetGridPane, 
            int columnIndex, int rowIndex, 
            ColumnArea targetColumnArea, RowArea targetRowArea) {
        assert targetGridPane != null;
        assert targetGridPane.getSceneGraphObject() instanceof GridPane;
        assert columnIndex >= 0;
        assert rowIndex >= 0;
        
        this.targetGridPane = targetGridPane;
        this.targetColumnIndex = columnIndex;
        this.targetRowIndex = rowIndex;
        this.targetColumnArea = targetColumnArea;
        this.targetRowArea = targetRowArea;
    }

    public int getTargetColumnIndex() {
        return targetColumnIndex;
    }

    public int getTargetRowIndex() {
        return targetRowIndex;
    }

    public ColumnArea getTargetColumnArea() {
        return targetColumnArea;
    }

    public RowArea getTargetRowArea() {
        return targetRowArea;
    }
    
    /*
     * AbstractDropTarget
     */
    @Override
    public FXOMObject getTargetObject() {
        return targetGridPane;
    }

    @Override
    public boolean acceptDragSource(AbstractDragSource dragSource) {
        assert dragSource != null;
        
        final boolean result;
        if (dragSource.getDraggedObjects().isEmpty()) {
            result = false;
        } else {
            final DesignHierarchyMask m = new DesignHierarchyMask(targetGridPane);
            if (m.isAcceptingSubComponent(dragSource.getDraggedObjects())) {
                final FXOMObject draggedObject0 = dragSource.getDraggedObjects().get(0);
                assert draggedObject0.getSceneGraphObject() instanceof Node;
                
                final Node draggedNode0 = (Node) draggedObject0.getSceneGraphObject();
                final Integer columIndexObj = GridPane.getColumnIndex(draggedNode0);
                final Integer rowIndexObj = GridPane.getRowIndex(draggedNode0);
                final int currentColumnIndex = (columIndexObj == null) ? 0 : columIndexObj;
                final int currentRowIndex = (rowIndexObj == null) ? 0 : rowIndexObj;
                
                final boolean sameContainer 
                        = targetGridPane == draggedObject0.getParentObject();
                final boolean sameColumnIndex
                        = targetColumnIndex == currentColumnIndex;
                final boolean sameRowIndex
                        = targetRowIndex == currentRowIndex;
                final boolean sameArea
                        = (targetColumnArea == ColumnArea.CENTER)
                        && (targetRowArea == RowArea.CENTER);
                        
                result = (sameContainer == false) 
                        || (sameColumnIndex == false)
                        || (sameRowIndex == false)
                        || (sameArea == false);
            } else {
                result = false;
            }
        }
        
        return result;
    }

    @Override
    public Job makeDropJob(AbstractDragSource dragSource, EditorController editorController) {
        assert acceptDragSource(dragSource); // (1)
        assert editorController != null;
        
        final boolean shouldRefreshSceneGraph = true;
        final BatchJob result = new BatchJob(editorController,
                shouldRefreshSceneGraph, dragSource.makeDropJobDescription());
        
        final List<FXOMObject> draggedObjects = dragSource.getDraggedObjects();
        final FXOMObject hitObject = dragSource.getHitObject();
        final FXOMObject currentParent = hitObject.getParentObject();
        final boolean reparenting = (currentParent != targetGridPane);
        final GridPane gridPane = (GridPane) targetGridPane.getSceneGraphObject();

        //  Steps:
        //
        //  1) snapshot grid related properties of dragged objects
        //      => this must be done here because they will be lost by #1
        //  2) clear the selection
        //  3) remove drag source objects from their current parent (if any)
        //  4) add new columns/rows in target grip pane as needed
        //  5) add drag source objects to this drop target
        //  6) restore grid related properties
        //  7) select the dragged objects
        //
        //  Note: if source and target parents are the same, skip #2,#3,#5 and #7
                        
        // Step #1
        final GridSnapshot gridSnapshot;
        if ((currentParent != null) 
                && (currentParent.getSceneGraphObject() instanceof GridPane)) {
            gridSnapshot = new GridSnapshot(draggedObjects);
        } else {
            gridSnapshot = new GridSnapshot(draggedObjects, 1);
        }
            
        if (reparenting) {
            
            // Step #2
            result.addSubJob(new ClearSelectionJob(editorController));
            
            // Step #3
            if (currentParent != null) {
                for (FXOMObject draggedObject : draggedObjects) {
                    result.addSubJob(new DeleteObjectJob(draggedObject,
                            editorController));
                }
            }
        }
        
        // Step #4
        final GridBounds snapshotBounds = gridSnapshot.getBounds();
        final int hitColumnIndex = gridSnapshot.getColumnIndex(hitObject);
        final int hitRowIndex = gridSnapshot.getRowIndex(hitObject);
        final int destColumnIndex = (targetColumnArea == ColumnArea.RIGHT) ? targetColumnIndex+1 : targetColumnIndex;
        final int destRowIndex = (targetRowArea == RowArea.BOTTOM) ? targetRowIndex+1 : targetRowIndex;
        final int columnDelta = destColumnIndex - hitColumnIndex;
        final int rowDelta = destRowIndex - hitRowIndex;
        final GridBounds adjustedBounds = snapshotBounds.move(columnDelta, rowDelta);
        
        // Step #4.1 : columns
        switch(targetColumnArea) {
            case LEFT: 
            case RIGHT: { // Insert columns at destColumnIndex
                final int insertCount = snapshotBounds.getColumnSpan();
                result.addSubJob(new InsertColumnJob(targetGridPane, 
                        destColumnIndex, insertCount, editorController));
                break;
            }
            case CENTER: {// Insert columns at right (first) and left ends if needed
                final int targetColumnCount = Deprecation.getGridPaneColumnCount(gridPane);
                if (adjustedBounds.getMaxColumnIndex() > targetColumnCount) {
                    final int insertCount = adjustedBounds.getMaxColumnIndex() - targetColumnCount;
                    result.addSubJob(new InsertColumnJob(targetGridPane, 
                            targetColumnCount, insertCount, editorController));
                }
                if (adjustedBounds.getMinColumnIndex() < 0) {
                    final int insertCount = -adjustedBounds.getMinColumnIndex();
                    result.addSubJob(new InsertColumnJob(targetGridPane, 
                            0, insertCount, editorController));
                }
                break;
            }
        }
        
        // Step #4.2 : rows
        switch(targetRowArea) {
            case TOP: 
            case BOTTOM: { // Insert rows at destRowIndex
                final int insertCount = snapshotBounds.getRowSpan();
                result.addSubJob(new InsertRowJob(targetGridPane, 
                        destRowIndex, insertCount, editorController));
                break;
            }
            case CENTER: { // Insert rows at bottom (first) and top ends if needed
                final int targetRowCount = Deprecation.getGridPaneRowCount(gridPane);
                if (adjustedBounds.getMaxRowIndex() > targetRowCount) {
                    final int insertCount = adjustedBounds.getMaxRowIndex() - targetRowCount;
                    result.addSubJob(new InsertRowJob(targetGridPane, 
                            targetRowCount, insertCount, editorController));
                }
                if (adjustedBounds.getMinRowIndex() < 0) {
                    final int insertCount = -adjustedBounds.getMinRowIndex();
                    result.addSubJob(new InsertRowJob(targetGridPane, 
                            0, insertCount, editorController));
                }
                break;
            }
        }
        
        if (reparenting) {
            
            // Step #5
            for (FXOMObject draggedObject : draggedObjects) {
                final Job j = new InsertAsSubComponentJob(draggedObject, 
                        targetGridPane, -1, editorController);
                result.addSubJob(j);
            }
        }
        
        // Step #6
        for (FXOMObject draggedObject : draggedObjects) {
            assert draggedObject instanceof FXOMInstance; // Because (1)
            result.addSubJob(new MoveCellContentJob((FXOMInstance) draggedObject,
                    columnDelta, rowDelta, editorController));
        }
        
        if (reparenting) {
            // Step #7
            result.addSubJob(new UpdateSelectionJob(draggedObjects, editorController));
        }
        
        assert result.isExecutable();
        
        return result;
    }
    
    @Override
    public boolean isSelectRequiredAfterDrop() {
        return true;
    }
    
}

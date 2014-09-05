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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.GridPaneDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.GridPaneDropTarget.ColumnArea;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.GridPaneDropTarget.RowArea;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPaneHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPanePring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPaneTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.RegionResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.BoundsUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 *
 */
public class GridPaneDriver extends AbstractNodeDriver {

    private static final double MATCH_DIST = 4;

    public GridPaneDriver(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    /*
     * AbstractNodeDriver
     */
    
    @Override
    public AbstractHandles<?> makeHandles(FXOMObject fxomObject) {
        assert fxomObject instanceof FXOMInstance;
        assert fxomObject.getSceneGraphObject() instanceof GridPane;
        return new GridPaneHandles(contentPanelController, (FXOMInstance)fxomObject);
    }

    @Override
    public AbstractPring<?> makePring(FXOMObject fxomObject) {
        assert fxomObject instanceof FXOMInstance;
        assert fxomObject.getSceneGraphObject() instanceof GridPane;
        return new GridPanePring(contentPanelController, (FXOMInstance) fxomObject);
    }

    @Override
    public AbstractTring<?> makeTring(AbstractDropTarget dropTarget) {
        assert dropTarget != null;
        assert dropTarget.getTargetObject() instanceof FXOMInstance;
        assert dropTarget.getTargetObject().getSceneGraphObject() instanceof GridPane;
        return new GridPaneTring(contentPanelController, (FXOMInstance) dropTarget.getTargetObject());
    }

    
    @Override
    public AbstractResizer<?> makeResizer(FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof GridPane;
        return new RegionResizer((Region) fxomObject.getSceneGraphObject());
    }
    
    
    @Override
    public AbstractDropTarget makeDropTarget(FXOMObject fxomObject, double sceneX, double sceneY) {
        assert fxomObject.getSceneGraphObject() instanceof GridPane;
        assert fxomObject instanceof FXOMInstance;
        
        final AbstractDropTarget result;
        
        final FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
        final GridPane gridPane = (GridPane) fxomInstance.getSceneGraphObject();
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);
        
        
        /*
         *
         *               
         *     |--------|-------------|-----------|-----------------------|--|
         *         0           1            2                 3            4
         *     +-------------------------------------------------------------+ -
         *     |                                                             | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | |
         *     |  |     |   |         |   |       |   |                   |  | | 0
         *     |  | 0,0 |   |   1,0   |   |  2,0  |   |        3,0        |  | |
         *     |  |     |   |         |   |       |   |                   |  | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | -
         *     |                                                             | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | |
         *     |  |     |   |         |   |       |   |                   |  | | 1
         *     |  | 0,1 |   |   1,1   |   |  2,1  |   |        3,1        |  | |
         *     |  |     |   |         |   |       |   |                   |  | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | -
         *     |         (A)                                                 | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | |
         *     |  |     |   |         |   |       |   |                   |  | | 2
         *     |  | 0,2 |   |   1,2   |   |  2,2  |   |        3,2        |  | |
         *     |  |     |   |   (B)   |   |       |   |                   |  | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | -
         *     |                                                             | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | |
         *     |  |     |   |         |   |       |   |                   |  | | 3
         *     |  | 0,3 |   |   1,3   |   |  2,3  |   |        3,3        |  | |
         *     |  |     |   |         |   |       |   |                   |  | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | -
         *     |                                                             | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | |
         *     |  |     |   |         |   |       |   |                   |  | | 4
         *     |  | 0,4 |   |   1,4   |   |  2,4  |   |        3,4        |  | |
         *     |  |     |   |         |   |       |   |                   |  | |
         *     |  +-----+   +---------+   +-------+   +-------------------+  | -
         *     |                                                             | | 5
         *     +-------------------------------------------------------------+ -
         */
        
        if ((rowCount == 0) || (columnCount == 0)) {
            result = new GridPaneDropTarget(fxomInstance, 0, 0, ColumnArea.CENTER, RowArea.CENTER);
        } else {
        
            final Point2D hitPoint = gridPane.sceneToLocal(sceneX, sceneY, true /* rootScene */);
            final double hitX = hitPoint.getX();
            final double hitY = hitPoint.getY();
            final int targetColumnIndex, targetRowIndex;
            final double targetCellX, targetCellY;
            
            // Searches the column where hitX resides
            int c = 0;
            Bounds cellBounds = Deprecation.getGridPaneCellBounds(gridPane, c++, 0);
            double columnMaxX = cellBounds.getMaxX();
            while ((columnMaxX < hitX) && (c < columnCount)) {
                cellBounds = Deprecation.getGridPaneCellBounds(gridPane, c++, 0);                
                columnMaxX = cellBounds.getMaxX();
            }
            if (hitX <= columnMaxX) { // hitX is in column 'c-1'
                assert hitX <= columnMaxX;
                targetColumnIndex = c-1;
                targetCellX = hitX;
            } else { // hitX is past the last column
                targetColumnIndex = columnCount;
                targetCellX = 0.0;
            }
            
            // Searches the row where hitY resides
            int r = 0;
            cellBounds = Deprecation.getGridPaneCellBounds(gridPane, 0, r++);
            double rowMaxY = cellBounds.getMaxY();
            while ((rowMaxY < hitY) && (r < rowCount)) {
                cellBounds = Deprecation.getGridPaneCellBounds(gridPane, 0, r++);
                rowMaxY = cellBounds.getMaxY();
            }
            if (hitY <= rowMaxY) { // hitY is in row 'r-1'
                assert hitY <= rowMaxY;
                targetRowIndex = r-1;
                targetCellY = hitY;
            } else {
                targetRowIndex = rowCount;
                targetCellY = 0.0;
            }
            
            
            /*
             *            hgap          targetCellBounds.width
             *        +----------+----------------------------------+
             *        |                                             |
             *        |                                             |
             *        +          +----------------------------------+
             *        |          |                                  |
             *        |          |    +........................+    |
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |    (A)   |(B) .           (C)          . (D)|
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |          |    +........................+    |
             *        |          |                                  |
             *        +----------+----------------------------------+
             * 
             *        (A) ColumnArea.LEFT
             *        (B) ColumnArea.LEFT
             *        (C) ColumnArea.CENTER
             *        (D) ColumnArea.RIGHT
             */
            
            final ColumnArea targetColumnArea;
            if (targetColumnIndex < columnCount) {
                final Bounds targetCellBounds 
                        = Deprecation.getGridPaneCellBounds(gridPane, targetColumnIndex, 0);
                final BoundsUtils.EdgeInfo edgeInfo
                        = BoundsUtils.distanceToEdges(targetCellBounds, targetCellX, targetCellY, gridPane);
                if (targetCellX < targetCellBounds.getMinX()) {
                    targetColumnArea= ColumnArea.LEFT; // (A)
                } else if (edgeInfo == null) {
                    targetColumnArea= ColumnArea.CENTER; // cell bounds are empty
                } else {
                    final boolean eastMatch = edgeInfo.getEastDistance() < MATCH_DIST;
                    final boolean westMatch = edgeInfo.getWestDistance() < MATCH_DIST;
                    if (westMatch) {
                        targetColumnArea= ColumnArea.LEFT; // (B)
                    } else if (eastMatch) {
                        targetColumnArea= ColumnArea.RIGHT; // (D)
                    } else {
                        targetColumnArea= ColumnArea.CENTER; // (C)
                    }
                }
            } else {
                targetColumnArea = ColumnArea.LEFT;
            }
            
            /*
             *        +----------+----------------------------------+
             *        |                                             |
             * vgap   |                           (A)               |
             *        |                                             |
             *        +          +----------------------------------+
             *        |          |                (B)               |
             *        |          |    +........................+    |
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |          |    .           (C)          .    |
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |          |    .                        .    |
             *        |          |    +........................+    |
             *        |          |                (D)               |
             *        +----------+----------------------------------+
             * 
             *        (A) RowArea.TOP
             *        (B) RowArea.TOP
             *        (C) RowArea.CENTER
             *        (D) RowArea.BOTTOM
             */
            
            final RowArea targetRowArea;
            if (targetRowIndex < rowCount) {
                final Bounds targetCellBounds 
                        = Deprecation.getGridPaneCellBounds(gridPane, 0, targetRowIndex);
                final BoundsUtils.EdgeInfo edgeInfo
                        = BoundsUtils.distanceToEdges(targetCellBounds, targetCellX, targetCellY, gridPane);
                if (targetCellY < targetCellBounds.getMinY()) {
                    targetRowArea = RowArea.TOP; // (A)
                } else if (edgeInfo == null) {
                    targetRowArea = RowArea.CENTER; // cell bounds are empty
                } else {
                    final boolean northMatch = edgeInfo.getNorthDistance() < MATCH_DIST;
                    final boolean southMatch = edgeInfo.getSouthDistance() < MATCH_DIST;
                    if (northMatch) {
                        targetRowArea= RowArea.TOP; // (B)
                    } else if (southMatch) {
                        targetRowArea= RowArea.BOTTOM; // (D)
                    } else {
                        targetRowArea= RowArea.CENTER; // (C)
                    }
                }
            } else {
                targetRowArea = RowArea.TOP;
            }
            
            result = new GridPaneDropTarget(fxomInstance, 
                    targetColumnIndex, targetRowIndex, 
                    targetColumnArea, targetRowArea);
        }
        
        
        return result;
    }
    
}

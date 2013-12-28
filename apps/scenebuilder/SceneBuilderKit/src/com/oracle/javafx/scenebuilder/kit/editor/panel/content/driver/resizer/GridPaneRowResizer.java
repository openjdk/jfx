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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer;

import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Bounds;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 *
 */
public class GridPaneRowResizer {
    
    private static final PropertyName minHeightName     = new PropertyName("minHeight"); //NOI18N
    private static final PropertyName prefHeightName    = new PropertyName("prefHeight"); //NOI18N
    private static final PropertyName maxHeightName     = new PropertyName("maxHeight"); //NOI18N
    private static final PropertyName percentHeightName = new PropertyName("percentHeight"); //NOI18N

    private final GridPane gridPane;
    private final int rowIndex;
    private final RowSizing originalSizing; // Row at rowIndex
    private final RowSizing originalSizingNext; // Row at rowIndex+1
    private final boolean usePercentSizing;
    private final double y1, y2, y3, y4, ym;

    public GridPaneRowResizer(GridPane gridPane, int rowIndex) {
        assert gridPane != null;
        assert rowIndex >= 0;
        assert rowIndex+1 < gridPane.getRowConstraints().size();
        
        this.gridPane = gridPane;
        this.rowIndex = rowIndex;
        this.originalSizing 
                = new RowSizing(gridPane.getRowConstraints().get(rowIndex));
        this.originalSizingNext 
                = new RowSizing(gridPane.getRowConstraints().get(rowIndex+1));
        this.usePercentSizing
                = countPercentWidths() == Deprecation.getGridPaneColumnCount(gridPane);
        
        //
        //
        //   y1 +-----
        //      |
        //      |
        //      |   row n
        //      |
        //      |
        //   y2 +-----
        //      |   vgap n
        ///  y3 +-----
        //      |
        //      |
        //      |
        //      |   row n+1
        //      |
        //   ym +
        //      |
        ///  y4 +-----
        //    
        //
        //       Range for moving y2 is [y1, ym]
        //       With (y4 - ym) == (y3 - y2)
        //

        // Compute y1, y2, y3, y4, ym
        final Bounds cellBounds 
                = Deprecation.getGridPaneCellBounds(gridPane, 0, rowIndex);
        final Bounds nextBounds 
                = Deprecation.getGridPaneCellBounds(gridPane, 0, rowIndex+1);

        y1 = cellBounds.getMinY();
        y2 = cellBounds.getMaxY();
        y3 = nextBounds.getMinY();
        y4 = nextBounds.getMaxY();
        ym = y4 - (y3 - y2);
        assert y1 <= ym;
    }

    public GridPane getSceneGraphObject() {
        return gridPane;
    }

    public int getColumnIndex() {
        return rowIndex;
    }
    
    public void updateHeight(double dy) {
        
        // Clamp y2 + dy in [y1, ym]
        final double newY2 = Math.max(y1, Math.min(ym, y2 + dy));
        final double newY3 = newY2 + (y3 - y2);
        final double newCellHeight = newY2 - y1;
        final double newNextHeight = y4 - newY3;
        
//        assert (newCellWidth+newNextWidth) == (downColWidths[colIndex]+downColWidths[colIndex+1]) :
//                "newCellWidth+newNextWidth=" +  (newCellWidth+newNextWidth) + ", " +
//                "downColWidths[colIndex]+downColWidths[colIndex+1]=" + 
//                (downColWidths[colIndex]+downColWidths[colIndex+1]);

        // Updates height of rows at rowIndex and rowIndex+1
        final RowConstraints rc = gridPane.getRowConstraints().get(rowIndex);
        final RowConstraints rcNext = gridPane.getRowConstraints().get(rowIndex+1);
        
        if (usePercentSizing) {
            final double ratio = newCellHeight / (ym - y1);
            
            final double base 
                    = originalSizing.getPercentHeight() 
                    + originalSizingNext.getPercentHeight();
            
            final double newPercentHeight = Math.floor(ratio * base);
            final double newPercentHeightNext = base - newPercentHeight;
            
            rc.setPercentHeight(newPercentHeight);
            rcNext.setPercentHeight(newPercentHeightNext);
            
        } else {
            
            // Row at rowIndex
            rc.setPrefHeight(newCellHeight);
            if (rc.getMinHeight() == Region.USE_COMPUTED_SIZE) {
                rc.setMinHeight(newCellHeight);
            } else {
                rc.setMinHeight(Math.min(newCellHeight, rc.getMinHeight()));
            }
            if (rc.getMaxHeight() == Region.USE_COMPUTED_SIZE) {
                rc.setMaxHeight(newCellHeight);
            } else {
                rc.setMaxHeight(Math.max(newCellHeight, rc.getMaxHeight()));
            }
            
            // Row at rowIndex+1
            rcNext.setPrefHeight(newNextHeight);
            if (rcNext.getMinHeight() == Region.USE_COMPUTED_SIZE) {
                rcNext.setMinHeight(newNextHeight);
            } else {
                rcNext.setMinHeight(Math.min(newNextHeight, rcNext.getMinHeight()));
            }
            if (rcNext.getMaxHeight() == Region.USE_COMPUTED_SIZE) {
                rcNext.setMaxHeight(newNextHeight);
            } else {
                rcNext.setMaxHeight(Math.max(newNextHeight, rcNext.getMaxHeight()));
            }
        }
        
    }
    
    public void revertToOriginalSize() {
        // Restore sizing of rows at rowIndex and rowIndex+1
        final RowConstraints cc = gridPane.getRowConstraints().get(rowIndex);
        final RowConstraints ccNext = gridPane.getRowConstraints().get(rowIndex+1);
        
        originalSizing.applyTo(cc);
        originalSizingNext.applyTo(ccNext);
    }
    
    
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        final RowConstraints cc = gridPane.getRowConstraints().get(rowIndex);
        if (MathUtils.equals(cc.getMinHeight(), originalSizing.getMinHeight()) == false) {
            result.put(minHeightName, cc.getMinHeight());
        }
        if (MathUtils.equals(cc.getPrefHeight(), originalSizing.getPrefHeight()) == false) {
            result.put(prefHeightName, cc.getPrefHeight());
        }
        if (MathUtils.equals(cc.getMaxHeight(), originalSizing.getMaxHeight()) == false) {
            result.put(maxHeightName, cc.getMaxHeight());
        }
        if (MathUtils.equals(cc.getPercentHeight(), originalSizing.getPercentHeight()) == false) {
            result.put(percentHeightName, cc.getPercentHeight());
        }
        return result;
    }
    
    
    public Map<PropertyName, Object> getChangeMapNext() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        final RowConstraints ccNext = gridPane.getRowConstraints().get(rowIndex+1);
        if (MathUtils.equals(ccNext.getMinHeight(), originalSizingNext.getMinHeight()) == false) {
            result.put(minHeightName, ccNext.getMinHeight());
        }
        if (MathUtils.equals(ccNext.getPrefHeight(), originalSizingNext.getPrefHeight()) == false) {
            result.put(prefHeightName, ccNext.getPrefHeight());
        }
        if (MathUtils.equals(ccNext.getMaxHeight(), originalSizingNext.getMaxHeight()) == false) {
            result.put(maxHeightName, ccNext.getMaxHeight());
        }
        if (MathUtils.equals(ccNext.getPercentHeight(), originalSizingNext.getPercentHeight()) == false) {
            result.put(percentHeightName, ccNext.getPercentHeight());
        }
        return result;
    }
    
    
    /*
     * Private
     */    
    
    private int countPercentWidths() {
        int result = 0;
        for (RowConstraints cc : gridPane.getRowConstraints()) {
            if (cc.getPercentHeight() != -1) {
                result++;
            }
        }
        return result;
    }
    
    
    private static class RowSizing {
        private final double minHeight;
        private final double maxHeight;
        private final double prefHeight;
        private final double percentHeight;
        
        public RowSizing(RowConstraints cc) {
            this.minHeight = cc.getMinHeight();
            this.maxHeight = cc.getMaxHeight();
            this.prefHeight = cc.getPrefHeight();
            this.percentHeight = cc.getPercentHeight();
        }

        public double getMinHeight() {
            return minHeight;
        }

        public double getMaxHeight() {
            return maxHeight;
        }

        public double getPrefHeight() {
            return prefHeight;
        }

        public double getPercentHeight() {
            return percentHeight;
        }

        public void applyTo(RowConstraints cc) {
            cc.setMinHeight(minHeight);
            cc.setMaxHeight(maxHeight);
            cc.setPrefHeight(prefHeight);
            cc.setPercentHeight(percentHeight);
        }
    }
}

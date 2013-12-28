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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 *
 */
public class GridPaneColumnResizer {
    
    private static final PropertyName minWidthName     = new PropertyName("minWidth"); //NOI18N
    private static final PropertyName prefWidthName    = new PropertyName("prefWidth"); //NOI18N
    private static final PropertyName maxWidthName     = new PropertyName("maxWidth"); //NOI18N
    private static final PropertyName percentWidthName = new PropertyName("percentWidth"); //NOI18N

    private final GridPane gridPane;
    private final int columnIndex;
    private final ColumnSizing originalSizing; // Column at columnIndex
    private final ColumnSizing originalSizingNext; // Column at columnIndex+1
    private final boolean usePercentSizing;
    private final double x1, x2, x3, x4, xm;

    public GridPaneColumnResizer(GridPane gridPane, int columnIndex) {
        assert gridPane != null;
        assert columnIndex >= 0;
        assert columnIndex+1 < gridPane.getColumnConstraints().size();
        
        this.gridPane = gridPane;
        this.columnIndex = columnIndex;
        this.originalSizing 
                = new ColumnSizing(gridPane.getColumnConstraints().get(columnIndex));
        this.originalSizingNext 
                = new ColumnSizing(gridPane.getColumnConstraints().get(columnIndex+1));
        this.usePercentSizing
                = countPercentWidths() == Deprecation.getGridPaneColumnCount(gridPane);
        
        //
        //         x1                x2         x3             xm        x4
        //       --+-----------------+----------+--------------+---------+--
        //         |      col n      |  hgap n  |         col n+1        |
        //         |                 |          |                        |
        //
        //       Range for moving x2 is [x1, xm]
        //       With (x4 - xm) == (x3 - x2)
        //

        final Bounds cellBounds 
                = Deprecation.getGridPaneCellBounds(gridPane, columnIndex, 0);
        final Bounds nextBounds 
                = Deprecation.getGridPaneCellBounds(gridPane, columnIndex+1, 0);
        x1 = cellBounds.getMinX();
        x2 = cellBounds.getMaxX();
        x3 = nextBounds.getMinX();
        x4 = nextBounds.getMaxX();
        xm = x4 - (x3 - x2);
        assert x1 <= xm;
    }

    public GridPane getSceneGraphObject() {
        return gridPane;
    }

    public int getColumnIndex() {
        return columnIndex;
    }
    
    public void updateWidth(double dx) {
        
        // Clamp x2 + dx in [x1, xm]
        final double newX2 = Math.max(x1, Math.min(xm, x2 + dx));
        final double newX3 = newX2 + (x3 - x2);
        final double newCellWidth = newX2 - x1;
        final double newNextWidth = x4 - newX3;
        
//        assert (newCellWidth+newNextWidth) == (downColWidths[colIndex]+downColWidths[colIndex+1]) :
//                "newCellWidth+newNextWidth=" +  (newCellWidth+newNextWidth) + ", " +
//                "downColWidths[colIndex]+downColWidths[colIndex+1]=" + 
//                (downColWidths[colIndex]+downColWidths[colIndex+1]);

        // Updates width of columns at columnIndex and columnIndex+1
        final ColumnConstraints cc = gridPane.getColumnConstraints().get(columnIndex);
        final ColumnConstraints ccNext = gridPane.getColumnConstraints().get(columnIndex+1);
        
        if (usePercentSizing) {
            final double ratio = newCellWidth / (xm - x1);
            
            final double base 
                    = originalSizing.getPercentWidth() 
                    + originalSizingNext.getPercentWidth();
            
            final double newPercentWidth = Math.floor(ratio * base);
            final double newPercentWidthNext = base - newPercentWidth;
            
            cc.setPercentWidth(newPercentWidth);
            ccNext.setPercentWidth(newPercentWidthNext);
            
        } else {
            
            // Column at columnIndex
            cc.setPrefWidth(newCellWidth);
            if (cc.getMinWidth() == Region.USE_COMPUTED_SIZE) {
                cc.setMinWidth(newCellWidth);
            } else {
                cc.setMinWidth(Math.min(newCellWidth, cc.getMinWidth()));
            }
            if (cc.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
                cc.setMaxWidth(newCellWidth);
            } else {
                cc.setMaxWidth(Math.max(newCellWidth, cc.getMaxWidth()));
            }
            
            // Column at columnIndex+1
            ccNext.setPrefWidth(newNextWidth);
            if (ccNext.getMinWidth() == Region.USE_COMPUTED_SIZE) {
                ccNext.setMinWidth(newNextWidth);
            } else {
                ccNext.setMinWidth(Math.min(newNextWidth, ccNext.getMinWidth()));
            }
            if (ccNext.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
                ccNext.setMaxWidth(newNextWidth);
            } else {
                ccNext.setMaxWidth(Math.max(newNextWidth, ccNext.getMaxWidth()));
            }
        }
        
        // Adjusts min
    }
    
    public void revertToOriginalSize() {
        // Restore sizing of columns at columnIndex and columnIndex+1
        final ColumnConstraints cc = gridPane.getColumnConstraints().get(columnIndex);
        final ColumnConstraints ccNext = gridPane.getColumnConstraints().get(columnIndex+1);
        
        originalSizing.applyTo(cc);
        originalSizingNext.applyTo(ccNext);
    }
    
    
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        final ColumnConstraints cc = gridPane.getColumnConstraints().get(columnIndex);
        if (MathUtils.equals(cc.getMinWidth(), originalSizing.getMinWidth()) == false) {
            result.put(minWidthName, cc.getMinWidth());
        }
        if (MathUtils.equals(cc.getPrefWidth(), originalSizing.getPrefWidth()) == false) {
            result.put(prefWidthName, cc.getPrefWidth());
        }
        if (MathUtils.equals(cc.getMaxWidth(), originalSizing.getMaxWidth()) == false) {
            result.put(maxWidthName, cc.getMaxWidth());
        }
        if (MathUtils.equals(cc.getPercentWidth(), originalSizing.getPercentWidth()) == false) {
            result.put(percentWidthName, cc.getPercentWidth());
        }
        return result;
    }
    
    
    public Map<PropertyName, Object> getChangeMapNext() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        final ColumnConstraints ccNext = gridPane.getColumnConstraints().get(columnIndex+1);
        if (MathUtils.equals(ccNext.getMinWidth(), originalSizingNext.getMinWidth()) == false) {
            result.put(minWidthName, ccNext.getMinWidth());
        }
        if (MathUtils.equals(ccNext.getPrefWidth(), originalSizingNext.getPrefWidth()) == false) {
            result.put(prefWidthName, ccNext.getPrefWidth());
        }
        if (MathUtils.equals(ccNext.getMaxWidth(), originalSizingNext.getMaxWidth()) == false) {
            result.put(maxWidthName, ccNext.getMaxWidth());
        }
        if (MathUtils.equals(ccNext.getPercentWidth(), originalSizingNext.getPercentWidth()) == false) {
            result.put(percentWidthName, ccNext.getPercentWidth());
        }
        return result;
    }
    
    
    /*
     * Private
     */    
    
    private int countPercentWidths() {
        int result = 0;
        for (ColumnConstraints cc : gridPane.getColumnConstraints()) {
            if (cc.getPercentWidth() != -1) {
                result++;
            }
        }
        return result;
    }
    
    
    private static class ColumnSizing {
        private final double minWidth;
        private final double maxWidth;
        private final double prefWidth;
        private final double percentWidth;
        
        public ColumnSizing(ColumnConstraints cc) {
            this.minWidth = cc.getMinWidth();
            this.maxWidth = cc.getMaxWidth();
            this.prefWidth = cc.getPrefWidth();
            this.percentWidth = cc.getPercentWidth();
        }

        public double getMinWidth() {
            return minWidth;
        }

        public double getMaxWidth() {
            return maxWidth;
        }

        public double getPrefWidth() {
            return prefWidth;
        }

        public double getPercentWidth() {
            return percentWidth;
        }

        public void applyTo(ColumnConstraints cc) {
            cc.setMinWidth(minWidth);
            cc.setMaxWidth(maxWidth);
            cc.setPrefWidth(prefWidth);
            cc.setPercentWidth(percentWidth);
        }
    }
}

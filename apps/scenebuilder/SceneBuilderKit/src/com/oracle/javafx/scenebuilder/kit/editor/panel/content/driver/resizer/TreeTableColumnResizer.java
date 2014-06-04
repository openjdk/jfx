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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Bounds;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.Region;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TreeTableViewDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;

/**
 *
 */
public class TreeTableColumnResizer {
    
    private static final PropertyName minWidthName     = new PropertyName("minWidth"); //NOI18N
    private static final PropertyName prefWidthName    = new PropertyName("prefWidth"); //NOI18N
    private static final PropertyName maxWidthName     = new PropertyName("maxWidth"); //NOI18N

    private final TreeTableColumn<?,?> treeTableColumn;
    private final TreeTableColumn<?,?> treeTableColumnNext;
    private final ColumnSizing originalSizing; // Column at columnIndex
    private final ColumnSizing originalSizingNext; // Column at columnIndex+1 (if any)
    private final double x1, x2, x3;

    public TreeTableColumnResizer(TreeTableColumn<?,?> treeTableColumn) {
        assert treeTableColumn != null;
        assert treeTableColumn.getTreeTableView() != null;
        
        this.treeTableColumn = treeTableColumn;
        this.originalSizing = new ColumnSizing(this.treeTableColumn);
        
        final List<?> columns;
        if (this.treeTableColumn.getParentColumn() != null) {
            columns = this.treeTableColumn.getParentColumn().getColumns();
        } else {
            columns = this.treeTableColumn.getTreeTableView().getColumns();
        }
        final int columnIndex = columns.indexOf(this.treeTableColumn);
        if (columnIndex+1 < columns.size()) {
            this.treeTableColumnNext = (TreeTableColumn<?,?>)columns.get(columnIndex+1);
            this.originalSizingNext = new ColumnSizing(this.treeTableColumnNext);
        } else {
            this.treeTableColumnNext = null;
            this.originalSizingNext = null;
        }
        
        //
        //  Case #1 : treeTableColumnNext != null
        //
        //         x1                x2                       x3
        //       --+-----------------+------------------------+--
        //         |      col n      |         col n+1        |
        //         |                 |                        |
        //
        //       Range for moving x2 is [x1, x3]
        //
        //
        //  Case #2 : treeTableColumnNext == null
        //
        //      Case #2.1: treeTableColumn.getParentColumn() != null
        //
        //         x1                x2                       x3
        //       --+-----------------+                        |
        //         |      col n      |                        |
        //         |                 |                        |
        //                                               parentColumn maxX
        //
        //
        //      Case #2.2: treeTableColumn.getParentColumn() == null
        //
        //         x1                x2                       x3
        //       --+-----------------+                        |
        //         |      col n      |                        |
        //         |                 |                        |
        //                                               treeTableView maxX
        // 
        //       Range for moving x2 is [x1, x3]
        //
        //

        final TreeTableViewDesignInfoX di = new TreeTableViewDesignInfoX();
        final Bounds columnBounds = di.getColumnBounds(treeTableColumn);
        x1 = columnBounds.getMinX();
        x2 = columnBounds.getMaxX();
        if (treeTableColumnNext != null) {
            final Bounds nextBounds = di.getColumnBounds(treeTableColumnNext);
            x3 = nextBounds.getMaxX();
        } else {
            if (treeTableColumn.getParentColumn() != null) {
                final TableColumnBase<?,?> parentColumn 
                        = (TableColumnBase<?,?>) this.treeTableColumn.getParentColumn();
                assert parentColumn instanceof TreeTableColumn<?,?>;
                final Bounds parentBounds = di.getColumnBounds((TreeTableColumn<?,?>)parentColumn);
                x3 = parentBounds.getMaxX();
            } else {
                final Bounds layoutBounds = treeTableColumn.getTreeTableView().getLayoutBounds();
                x3 = layoutBounds.getMaxX();
            }
        }
    }

    public TreeTableColumn<?,?> getTreeTableColumn() {
        return treeTableColumn;
    }
    
    public void updateWidth(double dx) {
        
        // Clamp x2 + dx in [x1, x3]
        final double newX2 = Math.max(x1, Math.min(x3, x2 + dx));
        final double newWidth = newX2 - x1;
        final double newWidthNext = x3 - newX2;
        
//        assert (newCellWidth+newNextWidth) == (downColWidths[colIndex]+downColWidths[colIndex+1]) :
//                "newCellWidth+newNextWidth=" +  (newCellWidth+newNextWidth) + ", " +
//                "downColWidths[colIndex]+downColWidths[colIndex+1]=" + 
//                (downColWidths[colIndex]+downColWidths[colIndex+1]);

        // Updates width of treeTableColumn 
        treeTableColumn.setPrefWidth(newWidth);
        if (treeTableColumn.getMinWidth() == Region.USE_COMPUTED_SIZE) {
            treeTableColumn.setMinWidth(newWidth);
        } else {
            treeTableColumn.setMinWidth(Math.min(newWidth, treeTableColumn.getMinWidth()));
        }
        if (treeTableColumn.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
            treeTableColumn.setMaxWidth(newWidth);
        } else {
            treeTableColumn.setMaxWidth(Math.max(newWidth, treeTableColumn.getMaxWidth()));
        }
        
        // Updates with of treeTableColumnNext
        if (treeTableColumnNext != null) {
            treeTableColumnNext.setPrefWidth(newWidthNext);
            if (treeTableColumnNext.getMinWidth() == Region.USE_COMPUTED_SIZE) {
                treeTableColumnNext.setMinWidth(newWidthNext);
            } else {
                treeTableColumnNext.setMinWidth(Math.min(newWidthNext, treeTableColumnNext.getMinWidth()));
            }
            if (treeTableColumnNext.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
                treeTableColumnNext.setMaxWidth(newWidthNext);
            } else {
                treeTableColumnNext.setMaxWidth(Math.max(newWidthNext, treeTableColumnNext.getMaxWidth()));
            }
        }
    }
    
    public void revertToOriginalSize() {
        originalSizing.applyTo(treeTableColumn);
        if (treeTableColumnNext != null) {
            originalSizingNext.applyTo(treeTableColumnNext);
        }
    }
    
    
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        if (MathUtils.equals(treeTableColumn.getMinWidth(), originalSizing.getMinWidth()) == false) {
            result.put(minWidthName, treeTableColumn.getMinWidth());
        }
        if (MathUtils.equals(treeTableColumn.getPrefWidth(), originalSizing.getPrefWidth()) == false) {
            result.put(prefWidthName, treeTableColumn.getPrefWidth());
        }
        if (MathUtils.equals(treeTableColumn.getMaxWidth(), originalSizing.getMaxWidth()) == false) {
            result.put(maxWidthName, treeTableColumn.getMaxWidth());
        }
        return result;
    }
    
    
    public Map<PropertyName, Object> getChangeMapNext() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        if (treeTableColumnNext != null) {
            if (MathUtils.equals(treeTableColumnNext.getMinWidth(), originalSizingNext.getMinWidth()) == false) {
                result.put(minWidthName, treeTableColumnNext.getMinWidth());
            }
            if (MathUtils.equals(treeTableColumnNext.getPrefWidth(), originalSizingNext.getPrefWidth()) == false) {
                result.put(prefWidthName, treeTableColumnNext.getPrefWidth());
            }
            if (MathUtils.equals(treeTableColumnNext.getMaxWidth(), originalSizingNext.getMaxWidth()) == false) {
                result.put(maxWidthName, treeTableColumnNext.getMaxWidth());
            }
        }
        
        return result;
    }
    
    
    /*
     * Private
     */    
    
    private static class ColumnSizing {
        private final double minWidth;
        private final double maxWidth;
        private final double prefWidth;
        
        public ColumnSizing(TreeTableColumn<?,?> tc) {
            this.minWidth = tc.getMinWidth();
            this.maxWidth = tc.getMaxWidth();
            this.prefWidth = tc.getPrefWidth();
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

        public void applyTo(TreeTableColumn<?,?> tc) {
            tc.setMinWidth(minWidth);
            tc.setMaxWidth(maxWidth);
            tc.setPrefWidth(prefWidth);
        }
    }
}

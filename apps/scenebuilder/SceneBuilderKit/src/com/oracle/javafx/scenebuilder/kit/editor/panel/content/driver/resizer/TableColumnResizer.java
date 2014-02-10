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

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TableViewDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Bounds;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Region;

/**
 *
 */
public class TableColumnResizer {
    
    private static final PropertyName minWidthName     = new PropertyName("minWidth"); //NOI18N
    private static final PropertyName prefWidthName    = new PropertyName("prefWidth"); //NOI18N
    private static final PropertyName maxWidthName     = new PropertyName("maxWidth"); //NOI18N

    private final TableColumn<?,?> tableColumn;
    private final TableColumn<?,?> tableColumnNext;
    private final ColumnSizing originalSizing; // Column at columnIndex
    private final ColumnSizing originalSizingNext; // Column at columnIndex+1 (if any)
    private final double x1, x2, x3;

    public TableColumnResizer(TableColumn<?,?> tableColumn) {
        assert tableColumn != null;
        assert tableColumn.getTableView() != null;
        
        this.tableColumn = tableColumn;
        this.originalSizing = new ColumnSizing(this.tableColumn);
        
        final List<?> columns;
        if (this.tableColumn.getParentColumn() != null) {
            columns = this.tableColumn.getParentColumn().getColumns();
        } else {
            columns = this.tableColumn.getTableView().getColumns();
        }
        final int columnIndex = columns.indexOf(this.tableColumn);
        if (columnIndex+1 < columns.size()) {
            this.tableColumnNext = (TableColumn<?,?>)columns.get(columnIndex+1);
            this.originalSizingNext = new ColumnSizing(this.tableColumnNext);
        } else {
            this.tableColumnNext = null;
            this.originalSizingNext = null;
        }
        
        //
        //  Case #1 : tableColumnNext != null
        //
        //         x1                x2                       x3
        //       --+-----------------+------------------------+--
        //         |      col n      |         col n+1        |
        //         |                 |                        |
        //
        //       Range for moving x2 is [x1, x3]
        //
        //
        //  Case #2 : tableColumnNext == null
        //       
        //       Case #2.1 : tableColumn.getParentColumn() != null
        //
        //         x1                x2                       x3
        //       --+-----------------+                        |
        //         |      col n      |                        |
        //         |                 |                        |
        //                                               parentColumn maxX
        // 
        //       Case #2.2 : tableColumn.getParentColumn() == null
        //
        //         x1                x2                       x3
        //       --+-----------------+                        |
        //         |      col n      |                        |
        //         |                 |                        |
        //                                               tableView maxX
        // 
        //       Range for moving x2 is [x1, x3]
        //
        //

        final TableViewDesignInfoX di = new TableViewDesignInfoX();
        final Bounds columnBounds = di.getColumnBounds(tableColumn);
        x1 = columnBounds.getMinX();
        x2 = columnBounds.getMaxX();
        if (tableColumnNext != null) { // Case #1
            final Bounds nextBounds = di.getColumnBounds(tableColumnNext);
            x3 = nextBounds.getMaxX();
        } else {
            if (tableColumn.getParentColumn() != null) { // Case #2.1
                final TableColumn<?,?> parentColumn = (TableColumn<?,?>) this.tableColumn.getParentColumn();
                final Bounds parentBounds = di.getColumnBounds(parentColumn);
                x3 = parentBounds.getMaxX();
            } else { // Case #2.2
                final Bounds layoutBounds = tableColumn.getTableView().getLayoutBounds();
                x3 = layoutBounds.getMaxX();
            }
        }
    }

    public TableColumn<?,?> getTableColumn() {
        return tableColumn;
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

        // Updates width of tableColumn 
        tableColumn.setPrefWidth(newWidth);
        if (tableColumn.getMinWidth() == Region.USE_COMPUTED_SIZE) {
            tableColumn.setMinWidth(newWidth);
        } else {
            tableColumn.setMinWidth(Math.min(newWidth, tableColumn.getMinWidth()));
        }
        if (tableColumn.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
            tableColumn.setMaxWidth(newWidth);
        } else {
            tableColumn.setMaxWidth(Math.max(newWidth, tableColumn.getMaxWidth()));
        }
        
        // Updates with of tableColumNext
        if (tableColumnNext != null) {
            tableColumnNext.setPrefWidth(newWidthNext);
            if (tableColumnNext.getMinWidth() == Region.USE_COMPUTED_SIZE) {
                tableColumnNext.setMinWidth(newWidthNext);
            } else {
                tableColumnNext.setMinWidth(Math.min(newWidthNext, tableColumnNext.getMinWidth()));
            }
            if (tableColumnNext.getMaxWidth() == Region.USE_COMPUTED_SIZE) {
                tableColumnNext.setMaxWidth(newWidthNext);
            } else {
                tableColumnNext.setMaxWidth(Math.max(newWidthNext, tableColumnNext.getMaxWidth()));
            }
        }
    }
    
    public void revertToOriginalSize() {
        originalSizing.applyTo(tableColumn);
        if (tableColumnNext != null) {
            originalSizingNext.applyTo(tableColumnNext);
        }
    }
    
    
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        if (MathUtils.equals(tableColumn.getMinWidth(), originalSizing.getMinWidth()) == false) {
            result.put(minWidthName, tableColumn.getMinWidth());
        }
        if (MathUtils.equals(tableColumn.getPrefWidth(), originalSizing.getPrefWidth()) == false) {
            result.put(prefWidthName, tableColumn.getPrefWidth());
        }
        if (MathUtils.equals(tableColumn.getMaxWidth(), originalSizing.getMaxWidth()) == false) {
            result.put(maxWidthName, tableColumn.getMaxWidth());
        }
        return result;
    }
    
    
    public Map<PropertyName, Object> getChangeMapNext() {
        final Map<PropertyName, Object> result = new HashMap<>();
        
        if (tableColumnNext != null) {
            if (MathUtils.equals(tableColumnNext.getMinWidth(), originalSizingNext.getMinWidth()) == false) {
                result.put(minWidthName, tableColumnNext.getMinWidth());
            }
            if (MathUtils.equals(tableColumnNext.getPrefWidth(), originalSizingNext.getPrefWidth()) == false) {
                result.put(prefWidthName, tableColumnNext.getPrefWidth());
            }
            if (MathUtils.equals(tableColumnNext.getMaxWidth(), originalSizingNext.getMaxWidth()) == false) {
                result.put(maxWidthName, tableColumnNext.getMaxWidth());
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
        
        public ColumnSizing(TableColumn<?,?> tc) {
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

        public void applyTo(TableColumn<?,?> tc) {
            tc.setMinWidth(minWidth);
            tc.setMaxWidth(maxWidth);
            tc.setPrefWidth(prefWidth);
        }
    }
}

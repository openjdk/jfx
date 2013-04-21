/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene.control.skin;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableColumnBase;
import javafx.util.Duration;

import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import com.sun.javafx.tk.Toolkit;

/**
 */
public abstract class TableRowSkinBase<T, 
                                       C extends IndexedCell/*<T>*/, 
                                       B extends CellBehaviorBase<C>, 
                                       R extends IndexedCell> extends CellSkinBase<C,B> {
    
    /*
     * This is rather hacky - but it is a quick workaround to resolve the
     * issue that we don't know maximum width of a disclosure node for a given
     * TreeView. If we don't know the maximum width, we have no way to ensure
     * consistent indentation for a given TreeView.
     *
     * To work around this, we create a single WeakHashMap to store a max
     * disclosureNode width per TreeView. We use WeakHashMap to help prevent
     * any memory leaks.
     */
    static final Map<Control, Double> maxDisclosureWidthMap = new WeakHashMap<Control, Double>();
    
    protected int getIndentationLevel(C control) {
        // TreeTableView.getNodeLevel(control.getTreeTable)
        return 0;
    }
    
    protected double getIndentationPerLevel() {
        return 0;
    }
    
    /**
     * Used to represent whether the current virtual flow owner is wanting 
     * indentation to be used in this table row. 
     */
    protected boolean isIndentationRequired() {
        return false;
    }
    
    /**
     * Returns the table column that should show the disclosure nodes and / or
     * a graphic. By default this is the left-most column.
     */
    protected TableColumnBase getTreeColumn() {
        return null;
    }

    protected Node getDisclosureNode() {
        return null;
    }
    
    /**
     * Used to represent whether a disclosure node is visible for _this_ 
     * table row. Not to be confused with isIndentationRequired(), which is the
     * more general API.
     */
    protected boolean isDisclosureNodeVisible() {
        // disclosureNode != null && treeItem != null && ! treeItem.isLeaf();
        return false;
    }
    
    protected boolean isShowRoot() {
        return true;
    }
    
    /**
     * Returns the graphic to draw on the inside of the disclosure node. Null
     * is acceptable when no graphic should be shown. Commonly this is the 
     * graphic associated with a TreeItem (i.e. treeItem.getGraphic()), rather
     * than a graphic associated with a cell.
     */
//    protected abstract Node getGraphic(); 
    protected abstract ObjectProperty<Node> graphicProperty();
    
    protected abstract Control getVirtualFlowOwner(); // return TableView / TreeTableView
    
    protected abstract ObservableList<? extends TableColumnBase/*<T,?>*/> getVisibleLeafColumns();
//    protected abstract ObjectProperty<SpanModel<T>> spanModelProperty();
    
    protected abstract void updateCell(R cell, C row);  // cell.updateTableRow(skinnable); (i.e cell.updateTableRow(row))
    
    protected abstract ObjectProperty<ObservableList<T>> itemsProperty();
    
    protected abstract boolean isColumnPartiallyOrFullyVisible(TableColumnBase tc); // tableViewSkin.isColumnPartiallyOrFullyVisible(tc)

    protected abstract R getCell(TableColumnBase tc);
    
    protected abstract TableColumnBase<T,?> getTableColumnBase(R cell);
    
    protected TableColumnBase<T,?> getVisibleLeafColumn(int column) {
        final List<? extends TableColumnBase/*<T,?>*/> visibleLeafColumns = getVisibleLeafColumns();
        if (column < 0 || column >= visibleLeafColumns.size()) return null;
        return visibleLeafColumns.get(column);
    }
    
//    private static enum SpanType {
//        NONE,
//        COLUMN,
//        ROW,
//        BOTH,
//        UNSET;
//    }
    
    // Specifies the number of times we will call 'recreateCells()' before we blow
    // out the cellsMap structure and rebuild all cells. This helps to prevent
    // against memory leaks in certain extreme circumstances.
    private static final int DEFAULT_FULL_REFRESH_COUNTER = 100;

    /*
     * A map that maps from TableColumn to TableCell (i.e. model to view).
     * This is recreated whenever the leaf columns change, however to increase
     * efficiency we create cells for all columns, even if they aren't visible,
     * and we only create new cells if we don't already have it cached in this
     * map.
     *
     * Note that this means that it is possible for this map to therefore be
     * a memory leak if an application uses TableView and is creating and removing
     * a large number of tableColumns. This is mitigated in the recreateCells()
     * function below - refer to that to learn more.
     */
    protected WeakHashMap<TableColumnBase, R> cellsMap;

    // This observableArrayList contains the currently visible table cells for this row.
    protected final List<R> cells = new ArrayList<R>();
    
    private int fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;

    protected boolean isDirty = false;
    protected boolean updateCells = false;
    
    private final double fixedCellLength;
    private final boolean fixedCellLengthEnabled;
    
    private ListChangeListener<TableColumnBase> visibleLeafColumnsListener = new ListChangeListener<TableColumnBase>() {
        @Override public void onChanged(Change<? extends TableColumnBase> c) {
            isDirty = true;
            getSkinnable().requestLayout();
        }
    };
    
    private WeakListChangeListener<TableColumnBase> weakVisibleLeafColumnsListener = 
            new WeakListChangeListener<TableColumnBase>(visibleLeafColumnsListener);
    
//    // spanning support
//    protected SpanModel spanModel;
    
//    // supports variable row heights
//    public static <C extends IndexedCell> double getTableRowHeight(int index, C tableRow) {
//        if (index < 0) {
//            return DEFAULT_CELL_SIZE;
//        }
//        
//        Group virtualFlowSheet = (Group) tableRow.getParent();
//        Node node = tableRow.getParent().getParent().getParent();
//        if (node instanceof VirtualFlow) {
//            ObservableList<Node> children = virtualFlowSheet.getChildren();
//            
//            if (index < children.size()) {
//                return children.get(index).prefHeight(tableRow.getWidth());
//            }
//        }
//        
//        return DEFAULT_CELL_SIZE;
//    }
//    
//    /**
//     * Used in layoutChildren to specify that the node is not visible due to spanning.
//     */
//    private void hide(Node node) {
//        node.setManaged(false);
//        node.setVisible(false);
//    }
    
    /**
     * Used in layoutChildren to specify that the node is now visible.
     */
    private void show(Node node) {
        node.setManaged(true);
        node.setVisible(true);
    }
    
//    // TODO we can optimise this code if we cache the spanTypeArray, which at
//    //      present is created for every query
//    // TODO we can optimise this code if we set a maximum span distance
//    private SpanType getSpanType(final int row, final int column) {
//        SpanType[][] spanTypeArray;
////        if (spanMap.containsKey(tableView)) {
////            spanTypeArray = spanMap.get(tableView);
////            
////            // if we already have an array, lets check it for the result
////            if (spanTypeArray != null && row < spanTypeArray.length && column < spanTypeArray[0].length) {
////                SpanType cachedResult = spanTypeArray[row][column];
////                if (cachedResult != SpanType.UNSET) {
////                    return cachedResult;
////                }
////            }
////        } else {
//            int rowCount = itemsProperty().get().size();
//            int columnCount = getVisibleLeafColumns().size();
//            spanTypeArray = new SpanType[rowCount][columnCount];
////            spanMap.put(tableView, spanTypeArray);
//            
//            // initialise the array to be SpanType.UNSET
//            for (int _row = 0; _row < rowCount; _row++) {
//                for (int _column = 0; _column < columnCount; _column++) {
//                    spanTypeArray[_row][_column] = SpanType.UNSET;
//                }
//            }
////        }
//        
//        if (spanModel == null) {
//            spanTypeArray[row][column] = SpanType.NONE;
//            return SpanType.NONE;
//        }
//        
//        // for the given row / column position, we need to see if anything in
//        // the spanModel will prevent this column from being shown
//        
//        // Firstly we will check along the x-axis (i.e. whether there is an
//        // earlier TableColumn that covers this column index)
//        int distance = 0;
//        for (int _col = column - 1; _col >= 0; _col--) {
//            distance++;
//            CellSpan cellSpan = getCellSpanAt(spanModel, row, _col);
//            if (cellSpan == null) continue;
//            if (cellSpan.getColumnSpan() > distance) {
//                spanTypeArray[row][column] = SpanType.COLUMN;
//                return SpanType.COLUMN;
//            }
//        }
//        
//        // secondly we'll try along the y-axis
//        distance = 0;
//        for (int _row = row - 1; _row >= 0; _row--) {
//            distance++;
//            CellSpan cellSpan = getCellSpanAt(spanModel, _row, column);
//            if (cellSpan == null) continue;
//            if (cellSpan.getRowSpan() > distance) {
//                spanTypeArray[row][column] = SpanType.ROW;
//                return SpanType.ROW;
//            }
//        }
//        
//        // finally, we have to try diagonally
//        int rowDistance = 0;
//        int columnDistance = 0;
//        for (int _col = column - 1, _row = row - 1; _col >= 0 && _row >= 0; _col--, _row--) {
//            rowDistance++;
//            columnDistance++;
//            CellSpan cellSpan = getCellSpanAt(spanModel, _row, _col);
//            if (cellSpan == null) continue;
//            if (cellSpan.getRowSpan() > rowDistance && 
//                cellSpan.getColumnSpan() > columnDistance) {
//                    spanTypeArray[row][column] = SpanType.BOTH;
//                    return SpanType.BOTH;
//            }
//        }
//        
//        spanTypeArray[row][column] = SpanType.NONE;
//        return SpanType.NONE;
//    }
    
    
    
    public TableRowSkinBase(C control, B behavior) {
        super(control, behavior);
        
        // TEMPORARY CODE (RT-24975)
        // we check the TableView to see if a fixed cell length is specified
        ObservableMap p = control.getProperties();
        String k = VirtualFlow.FIXED_CELL_LENGTH_KEY;
        fixedCellLength = (Double) (p.containsKey(k) ? p.get(k) : 0.0);
        fixedCellLengthEnabled = fixedCellLength > 0;
        // --- end of TEMPORARY CODE
        
        // init(control) should not be called here - it should be called by the
        // subclass after initialising itself. This is to prevent NPEs (for 
        // example, getVisibleLeafColumns() throws a NPE as the control itself
        // is not yet set in subclasses).
    }
    
    protected void init(C control) {
        getSkinnable().setPickOnBounds(false);
        
        recreateCells();
        updateCells(true);

        // init bindings
        // watches for any change in the leaf columns observableArrayList - this will indicate
        // that the column order has changed and that we should update the row
        // such that the cells are in the new order
        getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);
        // --- end init bindings
        
//        registerChangeListener(control.textProperty(), "TEXT");
//        registerChangeListener(control.graphicProperty(), "GRAPHIC");
//        registerChangeListener(control.editingProperty(), "EDITING");
        registerChangeListener(control.itemProperty(), "ITEM");
        
//        // add listener to cell span model
//        spanModel = spanModelProperty().get();
//        registerChangeListener(spanModelProperty(), "SPAN_MODEL");
    }

    @Override protected void handleControlPropertyChanged(String p) {
//        // we run this before the super call because we want to update whether
//        // we are showing columns or the node (if it isn't null) before the
//        // parent class updates the content
//        if ("TEXT".equals(p) || "GRAPHIC".equals(p) || "EDITING".equals(p)) {
//            updateShowColumns();
//        }

        super.handleControlPropertyChanged(p);

        if ("ITEM".equals(p)) {
            updateCells = true;
            getSkinnable().requestLayout();
//        } else if (p == "SPAN_MODEL") {
//            // TODO update layout based on changes to span model
//            spanModel = spanModelProperty().get();
//            getSkinnable().requestLayout();
        }
    }

    @Override protected void layoutChildren(double x, final double y,
            final double w, final double h) {
        
        checkState(true);
        if (cellsMap.isEmpty()) return;
        
        ObservableList<? extends TableColumnBase> visibleLeafColumns = getVisibleLeafColumns();
        if (! visibleLeafColumns.isEmpty()) {
            
            ///////////////////////////////////////////
            // indentation code starts here
            ///////////////////////////////////////////
            double leftMargin = 0;
            double disclosureWidth = 0;
            double graphicWidth = 0;
            boolean indentationRequired = isIndentationRequired();
            boolean disclosureVisible = isDisclosureNodeVisible();
            int indentationColumnIndex = 0;
            Node disclosureNode = null;
            if (indentationRequired) {
                // Determine the column in which we want to put the disclosure node.
                // By default it is null, which means the 0th column should be
                // where the indentation occurs.
                TableColumnBase<?,?> treeColumn = getTreeColumn();
                indentationColumnIndex = treeColumn == null ? 0 : visibleLeafColumns.indexOf(treeColumn);
                indentationColumnIndex = indentationColumnIndex < 0 ? 0 : indentationColumnIndex;
                
                int indentationLevel = getIndentationLevel(getSkinnable());
                if (! isShowRoot()) indentationLevel--;
                final double indentationPerLevel = getIndentationPerLevel();
                leftMargin = indentationLevel * indentationPerLevel;
            
                // position the disclosure node so that it is at the proper indent
                Control c = getVirtualFlowOwner();
                final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(c) ?
                    maxDisclosureWidthMap.get(c) : 0;
                disclosureWidth = defaultDisclosureWidth;
                
                disclosureNode = getDisclosureNode();
                if (disclosureNode != null) {
                    disclosureNode.setVisible(disclosureVisible);
                    
                    if (disclosureVisible) {
                        disclosureWidth = disclosureNode.prefWidth(h);
                        if (disclosureWidth > defaultDisclosureWidth) {
                            maxDisclosureWidthMap.put(c, disclosureWidth);
                        }
                    }
                }
            }
            ///////////////////////////////////////////
            // indentation code ends here
            ///////////////////////////////////////////
            
            // layout the individual column cells
            double width;
            double height;
            
            double verticalPadding = snappedTopInset() + snappedBottomInset();
            double horizontalPadding = snappedLeftInset() + snappedRightInset();

            /**
             * RT-26743:TreeTableView: Vertical Line looks unfinished.
             * We used to not do layout on cells whose row exceeded the number
             * of items, but now we do so as to ensure we get vertical lines
             * where expected in cases where the vertical height exceeds the 
             * number of items.
             */
            int index = getSkinnable().getIndex();
            if (index < 0/* || row >= itemsProperty().get().size()*/) return;
            
            for (int column = 0, max = cells.size(); column < max; column++) {
                R tableCell = cells.get(column);
                TableColumnBase<T, ?> tableColumn = getTableColumnBase(tableCell);
                
                show(tableCell);
                
                width = snapSize(tableCell.prefWidth(-1)) - snapSize(horizontalPadding);
                height = Math.max(getSkinnable().getHeight(), tableCell.prefHeight(-1));
                height = snapSize(height) - snapSize(verticalPadding);
                
                boolean isVisible = true;
                if (fixedCellLengthEnabled) {
                    // we determine if the cell is visible, and if not we have the
                    // ability to take it out of the scenegraph to help improve 
                    // performance. However, we only do this when there is a 
                    // fixed cell length specified in the TableView. This is because
                    // when we have a fixed cell length it is possible to know with
                    // certainty the height of each TableCell - it is the fixed value
                    // provided by the developer, and this means that we do not have
                    // to concern ourselves with the possibility that the height
                    // may be variable and / or dynamic.
                    isVisible = isColumnPartiallyOrFullyVisible(tableColumn);
                } 

                if (isVisible) {
                    // not ideal to have to do this O(n) lookup, but compared
                    // to what we had previously this is still a massive step
                    // forward
                    if (fixedCellLengthEnabled && ! getChildren().contains(tableCell)) {
                        getChildren().add(tableCell);
                    }
                    
                    
                    
                    ///////////////////////////////////////////
                    // further indentation code starts here
                    ///////////////////////////////////////////
                    if (indentationRequired && column == indentationColumnIndex) {
                        if (disclosureVisible) {
                            double ph = disclosureNode.prefHeight(disclosureWidth);
                            
                            if (width < (disclosureWidth + leftMargin)) {
                                fadeOut(disclosureNode);
                            } else {
                                fadeIn(disclosureNode);
                                disclosureNode.resize(disclosureWidth, ph);
                                positionInArea(disclosureNode, x + leftMargin, y,
                                        disclosureWidth, h, /*baseline ignored*/0,
                                        HPos.CENTER, VPos.CENTER);
                                disclosureNode.toFront();
                            }
                        }
                        
                        // determine starting point of the graphic or cell node, and the
                        // remaining width available to them
                        ObjectProperty<Node> graphicProperty = graphicProperty();
                        Node graphic = graphicProperty == null ? null : graphicProperty.get();
                        
                        if (graphic != null) {
                            graphicWidth = graphic.prefWidth(-1) + 3;
                            
                            if (width < disclosureWidth + leftMargin + graphicWidth) {
                                fadeOut(graphic);
                            } else {
                                fadeIn(graphic);
                                positionInArea(graphic, x + leftMargin + disclosureWidth, y,
                                            disclosureWidth, h, /*baseline ignored*/0,
                                            HPos.CENTER, VPos.CENTER);
                                graphic.toFront();
                            }
                        }
                    }
                    ///////////////////////////////////////////
                    // further indentation code ends here
                    ///////////////////////////////////////////
                    
//                    ///////////////////////////////////////////
//                    // cell spanning code starts here
//                    ///////////////////////////////////////////
//                    if (spanModel != null) {
//                        // cell span check - basically, see if there is a cell span
//                        // impacting upon the cell at the given row / column index
//                        SpanType spanType = getSpanType(row, column);
//                        switch (spanType) {
//                            case ROW:
//                            case BOTH: x += width; // fall through is on purpose here
//                            case COLUMN:
//                                hide(tableCell);
//                                tableCell.resize(0, 0);
//                                tableCell.relocate(x, insets.getTop());
//                                continue;          // we don't want to fall through
//                                                   // infact, we return to the loop here
//                            case NONE:
//                            case UNSET:            // fall through and carry on
//                        }
//
//                        CellSpan cellSpan = getCellSpanAt(spanModel, row, column);
//                        if (cellSpan != null) {
//                            if (cellSpan.getColumnSpan() > 1) {
//                                // we need to span multiple columns, so we sum up
//                                // the width of the additional columns, adding it
//                                // to the width variable
//                                for (int i = 1, 
//                                        colSpan = cellSpan.getColumnSpan(), 
//                                        maxColumns = getChildren().size() - column; 
//                                        i < colSpan && i < maxColumns; i++) {
//                                    // calculate the width
//                                    Node adjacentNode = getChildren().get(column + i);
//                                    width += snapSize(adjacentNode.prefWidth(-1));
//                                }
//                            }
//
//                            if (cellSpan.getRowSpan() > 1) {
//                                // we need to span multiple rows, so we sum up
//                                // the height of the additional rows, adding it
//                                // to the height variable
//                                for (int i = 1; i < cellSpan.getRowSpan(); i++) {
//                                    // calculate the height
//                                    double rowHeight = getTableRowHeight(row + i, getSkinnable());
//                                    height += snapSize(rowHeight);
//                                }
//                            }
//                        }
//                    } 
//                    ///////////////////////////////////////////
//                    // cell spanning code ends here
//                    ///////////////////////////////////////////
                    
                    tableCell.resize(width, height);
                    tableCell.relocate(x, snappedTopInset());
                    
                    // Request layout is here as (partial) fix for RT-28684
                    tableCell.requestLayout();
                } else {
                    if (fixedCellLengthEnabled) {
                        // we only add/remove to the scenegraph if the fixed cell
                        // length support is enabled - otherwise we keep all
                        // TableCells in the scenegraph
                        getChildren().remove(tableCell);
                    }
                }
                       
                x += width;
            }
        } else {
            super.layoutChildren(x,y,w,h);
        }
    }
    
//    private CellSpan getCellSpanAt(SpanModel spanModel, int row, int column) {
//        T rowObject = itemsProperty().get().get(row);
//        TableColumnBase<T,?> tableColumn = getVisibleLeafColumn(column);
//        return spanModel.getCellSpanAt(row, column, rowObject, tableColumn);
//    }

    private int columnCount = 0;
    
    private void recreateCells() {
        // This function is smart in the sense that we don't recreate all
        // TableCell instances every time this function is called. Instead we
        // only create TableCells for TableColumns we haven't already encountered.
        // To avoid a potential memory leak (when the TableColumns in the
        // TableView are created/inserted/removed/deleted, we have a 'refresh
        // counter' that when we reach 0 will delete all cells in this row
        // and recreate all of them.
        
//        TableView<T> table = getSkinnable().getTableView();
//        if (table == null) {
        if (cellsMap != null) {
            
//            Set<Entry<TableColumnBase, R>> cells = cellsMap.entrySet();
//            for (Entry<TableColumnBase, R> entry : cells) {
//                R cell = entry.getValue();
//                cell.dispose();
//            }
            
            cellsMap.clear();
        }
//        return;
//        }
        
        ObservableList<? extends TableColumnBase/*<T,?>*/> columns = getVisibleLeafColumns();
        
        if (columns.size() != columnCount || fullRefreshCounter == 0 || cellsMap == null) {
            if (cellsMap != null) {
                cellsMap.clear();
            }
            cellsMap = new WeakHashMap<TableColumnBase, R>(columns.size());
            fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;
            getChildren().clear();
        }
        columnCount = columns.size();
        fullRefreshCounter--;
        
        for (TableColumnBase col : columns) {
            if (cellsMap.containsKey(col)) {
                continue;
            }
            
            // create a TableCell for this column and store it in the cellsMap
            // for future use
            createCell(col);
        }
    }

    protected void updateCells(boolean resetChildren) {
        // if clear isn't called first, we can run into situations where the
        // cells aren't updated properly.
        cells.clear();

        C skinnable = getSkinnable();
        int skinnableIndex = skinnable.getIndex();
        List<? extends TableColumnBase/*<T,?>*/> visibleLeafColumns = getVisibleLeafColumns();
        for (int i = 0, max = visibleLeafColumns.size(); i < max; i++) {
            TableColumnBase<T,?> col = visibleLeafColumns.get(i);
            R cell = cellsMap.get(col);
            if (cell == null) {
                // if the cell is null it means we don't have it in cache and
                // need to create it
                cell = createCell(col);
            }

            updateCell(cell, skinnable);
            cell.updateIndex(skinnableIndex);
            cells.add(cell);
        }

        // update children of each row
        if (! fixedCellLengthEnabled && resetChildren) {
            getChildren().setAll(cells);
        }
    }
    
    private R createCell(TableColumnBase col) {
        // we must create a TableCell for this table column
        R cell = getCell(col);

        // and store this in our HashMap until needed
        cellsMap.put(col, cell);
        
        return cell;
    }
    
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefWidth = 0.0F;

        List<? extends TableColumnBase/*<T,?>*/> visibleLeafColumns = getVisibleLeafColumns();
        for (int i = 0, max = visibleLeafColumns.size(); i < max; i++) {
            TableColumnBase<T,?> tableColumn = visibleLeafColumns.get(i);
            prefWidth += tableColumn.getWidth();
        }

        return prefWidth;
    }
    
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (fixedCellLengthEnabled) {
            return fixedCellLength;
        }
        
        // fix for RT-29080
        checkState(false);
        
        // Support for RT-18467: making it easier to specify a height for
        // cells via CSS, where the desired height is less than the height
        // of the TableCells. Essentially, -fx-cell-size is given higher
        // precedence now
        if (getCellSize() < CellSkinBase.DEFAULT_CELL_SIZE) {
            return getCellSize();
        }

        // FIXME according to profiling, this method is slow and should
        // be optimised
        double prefHeight = 0.0f;
        final int count = cells.size();
        for (int i=0; i<count; i++) {
            final R tableCell = cells.get(i);
            prefHeight = Math.max(prefHeight, tableCell.prefHeight(-1));
        }
        double ph = Math.max(prefHeight, Math.max(getCellSize(), getSkinnable().minHeight(-1)));
        
        return ph;
    }
    
    @Override protected double computeMinHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        if (fixedCellLengthEnabled) {
            return fixedCellLength;
        }
        
        // fix for RT-29080
        checkState(false);
        
        // Support for RT-18467: making it easier to specify a height for
        // cells via CSS, where the desired height is less than the height
        // of the TableCells. Essentially, -fx-cell-size is given higher
        // precedence now
        if (getCellSize() < CellSkinBase.DEFAULT_CELL_SIZE) {
            return getCellSize();
        }

        // FIXME according to profiling, this method is slow and should
        // be optimised
        double minHeight = 0.0f;
        final int count = cells.size();
        for (int i = 0; i < count; i++) {
            final R tableCell = cells.get(i);
            minHeight = Math.max(minHeight, tableCell.minHeight(-1));
        }
        return minHeight;
    }
    
    private void checkState(boolean doRecreateIfNecessary) {
        if (isDirty) {
            // doRecreateIfNecessary was added to resolve RT-29382, which was
            // introduced by the fix for RT-29080 above in computePrefHeight
            if (doRecreateIfNecessary) {
                recreateCells();
            }
            updateCells(true);
            isDirty = false;
        } else if (updateCells) {
            updateCells(true);
            updateCells = false;
        }
    }
    
    private static final Duration FADE_DURATION = Duration.millis(200);
    
    // There appears to be a memory leak when using the stub toolkit. Therefore,
    // to prevent tests from failing we disable the animations below when the
    // stub toolkit is being used.
    // Filed as RT-29163.
    private static boolean IS_STUB_TOOLKIT = Toolkit.getToolkit().toString().contains("StubToolkit");
    
    private void fadeOut(final Node node) {
        if (node.getOpacity() < 1.0) return;
        
        if (IS_STUB_TOOLKIT) {
            node.setOpacity(0);
            return;
        }
        
        final FadeTransition fader = new FadeTransition(FADE_DURATION, node);
        fader.setToValue(0.0);
        fader.play();
    }
    
    private void fadeIn(final Node node) {
        if (node.getOpacity() > 0.0) return;
        
        if (IS_STUB_TOOLKIT) {
            node.setOpacity(1);
            return;
        }
        
        final FadeTransition fader = new FadeTransition(FADE_DURATION, node);
        fader.setToValue(1.0);
        fader.play();
    }
}

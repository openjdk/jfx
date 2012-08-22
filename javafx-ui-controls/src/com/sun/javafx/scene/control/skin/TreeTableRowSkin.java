/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.preview.javafx.scene.control.TreeTableRow;
import com.preview.javafx.scene.control.TreeTableView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.collections.WeakListChangeListener;
import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import com.sun.javafx.scene.control.behavior.TreeTableRowBehavior;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

/**
 *
 */
public class TreeTableRowSkin<T> extends CellSkinBase<TreeTableRow<T>, CellBehaviorBase<TreeTableRow<T>>> {

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
    private static final Map<TreeView, Double> maxDisclosureWidthMap = new WeakHashMap<TreeView, Double>();

    /**
     * The amount of space to multiply by the treeItem.level to get the left
     * margin for this tree cell. This is settable from CSS
     */
    private DoubleProperty indent = null;
    public final void setIndent(double value) { indentProperty().set(value); }
    public final double getIndent() { return indent == null ? 10.0 : indent.get(); }
    public final DoubleProperty indentProperty() { 
        if (indent == null) {
            indent = new StyleableDoubleProperty(10.0) {
                @Override public Object getBean() {
                    return TreeTableRowSkin.this;
                }

                @Override public String getName() {
                    return "indent";
                }

                @Override public StyleableProperty getStyleableProperty() {
                    return TreeTableRowSkin.StyleableProperties.INDENT;
                }
            };
        }
        return indent; 
    }

    public TreeTableRowSkin(TreeTableRow<T> control) {
        super(control, new TreeTableRowBehavior<T>(control));
        
        updateDisclosureNode();
        
        recreateCells();
        updateCells(true);

        initBindings();

        registerChangeListener(control.editingProperty(), "EDITING");
        registerChangeListener(control.indexProperty(), "ROW");
        registerChangeListener(control.tableViewProperty(), "TABLE_VIEW");
        registerChangeListener(control.treeItemProperty(), "TREE_ITEM");
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        // we run this before the super call because we want to update whether
        // we are showing columns or the node (if it isn't null) before the
        // parent class updates the content
        if ("TEXT".equals(p) || "GRAPHIC".equals(p) || "EDITING".equals(p)) {
            updateShowColumns();
        }

        super.handleControlPropertyChanged(p);

        if ("ROW".equals(p)) {
            updateCells = true;
            requestLayout();
//        } else if ("TABLE_VIEW".equals(p)) {
//            for (int i = 0; i < getChildren().size(); i++) {
//                ((TableCell)getChildren().get(i)).updateTableView(getSkinnable().getTableView());
//            }
        } else if ("TREE_ITEM".equals(p)) {
            updateDisclosureNode();
        }
        
    }
    
    private void updateDisclosureNode() {
        if (getSkinnable().isEmpty()) return;

        Node disclosureNode = getSkinnable().getDisclosureNode();
        if (disclosureNode == null) return;
        
        TreeItem treeItem = getSkinnable().getTreeItem();
        
        boolean disclosureVisible = treeItem != null && ! treeItem.isLeaf();
        disclosureNode.setVisible(disclosureVisible);
            
        if (! disclosureVisible) {
            getChildren().remove(disclosureNode);
        } else if (disclosureNode.getParent() == null) {
            getChildren().add(disclosureNode);
            disclosureNode.toFront();
        } else {
            disclosureNode.toBack();
        }
    }

    private boolean childrenDirty = false;
    @Override protected void updateChildren() {
        super.updateChildren();
        updateDisclosureNode();
        
        if (childrenDirty) {
            childrenDirty = false;
            if (showColumns) {
                if (cells.isEmpty()) {
                    getChildren().clear();
                } else {
                    // TODO we can optimise this by only showing cells that are 
                    // visible based on the table width and the amount of horizontal
                    // scrolling.
                    getChildren().addAll(cells);
                }
            } else {
                getChildren().clear();

                if (!isIgnoreText() || !isIgnoreGraphic()) {
                    getChildren().add(getSkinnable());
                }
            }
        }
    }
    
    @Override protected void layoutChildren(double x, final double y,
            double w, final double h) {
        TreeItem treeItem = getSkinnable().getTreeItem();
        if (treeItem == null) return;
        
        TreeView tree = getSkinnable().getTreeTableView();
        if (tree == null) return;
        
        doUpdateCheck();

        Node disclosureNode = getSkinnable().getDisclosureNode();
        
        int level = TreeView.getNodeLevel(getSkinnable().getTreeItem());
        if (! tree.isShowRoot()) level--;
        double leftMargin = getIndent() * level;

        x += leftMargin;

        // position the disclosure node so that it is at the proper indent
        boolean disclosureVisible = disclosureNode != null && treeItem != null && ! treeItem.isLeaf();

        final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
            maxDisclosureWidthMap.get(tree) : 0;
        double disclosureWidth = defaultDisclosureWidth;

        if (disclosureVisible) {
            disclosureWidth = disclosureNode.prefWidth(-1);
            if (disclosureWidth > defaultDisclosureWidth) {
                maxDisclosureWidthMap.put(tree, disclosureWidth);
            }

            double ph = disclosureNode.prefHeight(-1);

            System.out.println("disclosure visible: " + disclosureWidth + ", " + ph);
            disclosureNode.resize(disclosureWidth, ph);
            positionInArea(disclosureNode, x, y,
                    disclosureWidth, h, /*baseline ignored*/0,
                    HPos.CENTER, VPos.CENTER);
        }

        // determine starting point of the graphic or cell node, and the
        // remaining width available to them
        final int padding = treeItem.getGraphic() == null ? 0 : 3;
        x += disclosureWidth + padding;
        w -= (leftMargin + disclosureWidth + padding);

//        layoutLabelInArea(x, y, w, h);
        
        
        
        
        
        
        
        
        TableView<T> table = getSkinnable().getTableView();
        if (table == null) return;
        if (cellsMap.isEmpty()) return;
        
        if (showColumns && ! table.getVisibleLeafColumns().isEmpty()) {
            // layout the individual column cells
            TableColumn<T,?> col;
            TableCell cell;
//            double x = getInsets().getLeft();
            double width;
            double height;
            List<TableColumn<T,?>> leafColumns = table.getVisibleLeafColumns();
            
            double verticalPadding = getInsets().getTop() + getInsets().getBottom();
            double horizontalPadding = getInsets().getLeft() + getInsets().getRight();
            
            for (int i = 0; i < leafColumns.size(); i++) {
                col = leafColumns.get(i);
                cell = cellsMap.get(col);
                if (cell == null) continue;

                width = snapSize(cell.prefWidth(-1) - horizontalPadding);
                height = Math.max(getHeight(), cell.prefHeight(-1));
                height = snapSize(height - verticalPadding);
                
                if (i == 0) {
                    cell.resize(width - leftMargin - disclosureWidth, height);
                    cell.relocate(x, getInsets().getTop());
                } else {
                    cell.resize(width, height);
                    cell.relocate(x - leftMargin - disclosureWidth, getInsets().getTop());
                }
                
                x += width;
            }
        } else {
            super.layoutChildren(x,y,w,h);
        }
    }

    @Override protected double computePrefHeight(double width) {
        doUpdateCheck();
        
        if (showColumns) {
            // FIXME according to profiling, this method is slow and should
            // be optimised
            double prefHeight = 0.0f;
            final int count = cells.size();
            for (int i=0; i<count; i++) {
                final TableCell tableCell = cells.get(i);
                prefHeight = Math.max(prefHeight, tableCell.prefHeight(-1));
            }
            return Math.max(prefHeight, Math.max(getCellSize(), getSkinnable().minHeight(-1)));
        } else {
            double pref = super.computePrefHeight(width);
            Node d = getSkinnable().getDisclosureNode();
            return (d == null) ? pref : Math.max(d.prefHeight(-1), pref);
        }
    }
    
    @Override protected double computePrefWidth(double height) {
        doUpdateCheck();
        
        if (showColumns) {
            double pw = getInsets().getLeft() + getInsets().getRight();
            
            TreeTableView tree = getSkinnable().getTreeTableView();
            if (tree == null) return pw;

            TreeItem treeItem = getSkinnable().getTreeItem();
            if (treeItem == null) return pw;
            
            // determine the amount of indentation
            int level = TreeView.getNodeLevel(treeItem);
            if (! tree.isShowRoot()) level--;
            pw += getIndent() * level;

            // include the disclosure node width
            Node disclosureNode = getSkinnable().getDisclosureNode();
            final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
                    maxDisclosureWidthMap.get(tree) : 0;
            pw += Math.max(defaultDisclosureWidth, disclosureNode.prefWidth(-1));

            // sum up width of all columns
            if (getSkinnable().getTableView() != null) {
                for (TableColumn<T,?> tableColumn : getSkinnable().getTableView().getVisibleLeafColumns()) {
                    pw += tableColumn.getWidth();
                }
            }

            return pw;
        } else {
            return super.computePrefWidth(height);
        }
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    /** @treatAsPrivate */
    private static class StyleableProperties {
        
        private static final StyleableProperty<TreeTableRow,Number> INDENT = 
            new StyleableProperty<TreeTableRow,Number>("-fx-indent",
                SizeConverter.getInstance(), 10.0) {
                    
            @Override public boolean isSettable(TreeTableRow n) {
                DoubleProperty p = ((TreeTableRowSkin) n.getSkin()).indentProperty();
                return p == null || !p.isBound();
            }

            @Override public WritableValue<Number> getWritableValue(TreeTableRow n) {
                final TreeTableRowSkin skin = (TreeTableRowSkin) n.getSkin();
                return skin.indentProperty();
            }
        };
        
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(CellSkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                INDENT
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return TreeTableRowSkin.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
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
    private WeakHashMap<TableColumn, TableCell> cellsMap;

    // This observableArrayList contains the currently visible table cells for this row.
    private final List<TableCell> cells = new ArrayList<TableCell>();
    
    private int fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;

    private boolean showColumns = true;
    
    private boolean isDirty = false;
    private boolean updateCells = false;
    
    private ListChangeListener visibleLeafColumnsListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            isDirty = true;
            requestLayout();
        }
    };

//    public TableRowSkin(TableRow<T> tableRow) {
//        super(tableRow, new CellBehaviorBase<TableRow<T>>(tableRow));
//
//        recreateCells();
//        updateCells(true);
//
//        initBindings();
//
//        registerChangeListener(tableRow.editingProperty(), "EDITING");
//        registerChangeListener(tableRow.indexProperty(), "ROW");
//        registerChangeListener(tableRow.tableViewProperty(), "TABLE_VIEW");
//    }

    private void updateShowColumns() {
        boolean newValue = (isIgnoreText() && isIgnoreGraphic());
        if (showColumns == newValue) return;
        
        showColumns = newValue;

        requestLayout();
    }
    
    private void initBindings() {
        // watches for any change in the leaf columns observableArrayList - this will indicate
        // that the column order has changed and that we should update the row
        // such that the cells are in the new order
        if (getSkinnable() == null) {
            throw new IllegalStateException("TableRowSkin does not have a Skinnable set to a TableRow instance");
        }
        if (getSkinnable().getTableView() == null) {
            throw new IllegalStateException("TableRow not have the TableView property set");
        }
        
        getSkinnable().getTableView().getVisibleLeafColumns().addListener(
                new WeakListChangeListener(visibleLeafColumnsListener));
    }
    
    private void doUpdateCheck() {
        if (isDirty) {
            recreateCells();
            updateCells(true);
            isDirty = false;
        } else if (updateCells) {
            updateCells(false);
            updateCells = false;
        }
    }

    private void recreateCells() {
        // This function is smart in the sense that we don't recreate all
        // TableCell instances every time this function is called. Instead we
        // only create TableCells for TableColumns we haven't already encountered.
        // To avoid a potential memory leak (when the TableColumns in the
        // TableView are created/inserted/removed/deleted, we have a 'refresh
        // counter' that when we reach 0 will delete all cells in this row
        // and recreate all of them.
        
        TableView<T> table = getSkinnable().getTableView();
        if (table == null) {
            clearCellsMap();
            return;
        }
        
        ObservableList<TableColumn<T,?>> columns = table.getVisibleLeafColumns();
        
        if (fullRefreshCounter == 0 || cellsMap == null) {
            clearCellsMap();
            cellsMap = new WeakHashMap<TableColumn, TableCell>(columns.size());
            fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;
        }
        fullRefreshCounter--;
        
        for (TableColumn col : columns) {
            if (cellsMap.containsKey(col)) {
                continue;
            }
            
            // we must create a TableCell for each table column
            TableCell cell = (TableCell) col.getCellFactory().call(col);

            // we set it's TableColumn and TableView
            cell.updateTableColumn(col);
            cell.updateTableView(table);

            // and store this in our HashMap until needed
            cellsMap.put(col, cell);
        }
    }
    
    private void clearCellsMap() {
        if (cellsMap != null) cellsMap.clear();
    }

    private void updateCells(boolean resetChildren) {
        // if delete isn't called first, we can run into situations where the
        // cells aren't updated properly.
        cells.clear();

        TableView<T> table = getSkinnable().getTableView();
        if (table != null) {
            for (TableColumn<T,?> col : table.getVisibleLeafColumns()) {
                TableCell cell = cellsMap.get(col);
                if (cell == null) continue;

                cell.updateIndex(getSkinnable().getIndex());
                cell.updateTableRow(getSkinnable());
                cells.add(cell);
            }
        }

        if (resetChildren) {
            childrenDirty = true;
            updateChildren();
        }
    }
}

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

import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;

/**
 */
public class TreeTableCellSkin<S,T> extends TableCellSkinBase<TreeTableCell<S,T>, TreeTableCellBehavior<S,T>> {
    
    private final TreeTableCell<S,T> treeTableCell;
    private final TreeTableColumn<S,T> tableColumn;
    
    public TreeTableCellSkin(TreeTableCell<S,T> treeTableCell) {
        super(treeTableCell, new TreeTableCellBehavior<S,T>(treeTableCell));
        
        this.treeTableCell = treeTableCell;
        this.tableColumn = treeTableCell.getTableColumn();
        
        super.init(treeTableCell);
    }

    @Override protected BooleanProperty columnVisibleProperty() {
        return tableColumn.visibleProperty();
    }

    @Override protected ReadOnlyDoubleProperty columnWidthProperty() {
        return tableColumn.widthProperty();
    }

    @Override protected double leftLabelPadding() {
        double leftPadding = super.leftLabelPadding();
        
        // RT-27167: we must take into account the disclosure node and the
        // indentation (which is not taken into account by the LabeledSkinBase.
        final double height = getCellSize();

        TreeTableCell<S,T> cell = getSkinnable();

        TreeTableColumn<S,T> tableColumn = cell.getTableColumn();
        if (tableColumn == null) return leftPadding;

        // check if this column is the TreeTableView treeColumn (i.e. the 
        // column showing the disclosure node and graphic).
        TreeTableView<S> treeTable = cell.getTreeTableView();
        if (treeTable == null) return leftPadding;

        int columnIndex = treeTable.getVisibleLeafIndex(tableColumn);

        TreeTableColumn<S,?> treeColumn = treeTable.getTreeColumn();
        if ((treeColumn == null && columnIndex != 0) || (treeColumn != null && ! tableColumn.equals(treeColumn))) {
            return leftPadding;
        }

        TreeTableRow<S> treeTableRow = cell.getTreeTableRow();
        if (treeTableRow == null) return leftPadding;

        TreeItem<S> treeItem = treeTableRow.getTreeItem();
        if (treeItem == null) return leftPadding;
        
        int nodeLevel = treeTable.getTreeItemLevel(treeItem);
        if (! treeTable.isShowRoot()) nodeLevel--;

        double indentPerLevel = 10;
        if (treeTableRow.getSkin() instanceof TreeTableRowSkin) {
            indentPerLevel = ((TreeTableRowSkin<?>)treeTableRow.getSkin()).getIndentationPerLevel();
        }
        leftPadding += nodeLevel * indentPerLevel;

        // add in the width of the disclosure node, if one exists
        Map<Control, Double> mdwp = TableRowSkinBase.maxDisclosureWidthMap;
        leftPadding += mdwp.containsKey(treeTable) ? mdwp.get(treeTable) : 0;

        // adding in the width of the graphic on the tree item
        Node graphic = treeItem.getGraphic();
        leftPadding += graphic == null ? 0 : graphic.prefWidth(height);
        
        return leftPadding;
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (isDeferToParentForPrefWidth) {
            // RT-27167: we must take into account the disclosure node and the
            // indentation (which is not taken into account by the LabeledSkinBase.
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
        return columnWidthProperty().get();
    }
}

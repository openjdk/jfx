/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;

/**
 * Utility methods to access package-private api in Table-related skins.
 */
public class TableSkinShim {

    /**
     * Returns the TableHeaderRow of the skin's table if that is of type TableViewSkinBase
     * or null if not.
     *
     * @param <T>
     * @param table the table to get the TableHeaderRow from
     * @return the tableHeaderRow of the table's skin or null if the skin not of type
     *    TableViewSkinBase
     */
    public static <T> TableHeaderRow getTableHeaderRow(TableView<T> table) {
        if (table.getSkin() instanceof TableViewSkinBase) {
            return getTableHeaderRow((TableViewSkinBase) table.getSkin());
        }
        return null;
    }

    /**
     * Returns the TableHeaderRow of the given skin.
     *
     * @param <T>
     * @param skin the skin to get the TableHeaderRow from
     * @return
     * @throws NullPointerException if skin is null
     */
    public static <T> TableHeaderRow getTableHeaderRow(TableViewSkinBase skin) {
        return skin.getTableHeaderRow();
    }

    /**
     * Returns the TableColumnHeader for the given column or null if not available.
     *
     * @param <T>
     * @param column
     * @return
     */
    public static <T> TableColumnHeader getColumnHeaderFor(TableColumn<T, ?> column) {
        TableView<T> table = column.getTableView();
        TableHeaderRow tableHeader = getTableHeaderRow(table);
        if (tableHeader != null) {
            return tableHeader.getColumnHeaderFor(column);
        }
        return null;
    }

    /**
     * Returns the VirtualFlow from the table's skin which must be of type
     * TableViewSkin.
     */
    public static VirtualFlow<?> getVirtualFlow(TableView<?> table) {
        TableViewSkin<?> skin = (TableViewSkin<?>) table.getSkin();
        return skin.getVirtualFlow();
    }

    /**
     * Returns the VirtualFlow from the table's skin which must be of type
     * TreeTableViewSkin.
     */
    public static VirtualFlow<?> getVirtualFlow(TreeTableView<?> table) {
        TreeTableViewSkin<?> skin = (TreeTableViewSkin<?>) table.getSkin();
        return skin.getVirtualFlow();
    }

//    public static <T> TableRow<T> getCell(TableView<T> table, int index) {
//        VirtualFlow flow = getVirtualFlow(table);
//        return (TableRow<T>) flow.getCell(index);
//    }
//
//    public static <T> TreeTableRow<T> getCell(TreeTableView<T> table, int index) {
//        VirtualFlow flow = getVirtualFlow(table);
//        return (TreeTableRow<T>) flow.getCell(index);
//    }
//----------------- Tree/TableRowSkin state

    public static <T> boolean isFixedCellSizeEnabled(TableRow<T> tableRow) {
        TableRowSkin<T> skin = (TableRowSkin<T>) tableRow.getSkin();
        return skin.fixedCellSizeEnabled;
    }

    public static <T> boolean isFixedCellSizeEnabled(TreeTableRow<T> tableRow) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) tableRow.getSkin();
        return skin.fixedCellSizeEnabled;
    }

    public static <T> boolean isDirty(TableRow<T> tableRow) {
        TableRowSkin<T> skin = (TableRowSkin<T>) tableRow.getSkin();
        return skin.isDirty();
    }

    public static <T> boolean isDirty(TreeTableRow<T> tableRow) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) tableRow.getSkin();
        return skin.isDirty();
    }

    public static <T> void setDirty(TableRow<T> tableRow, boolean dirty) {
        TableRowSkin<T> skin = (TableRowSkin<T>) tableRow.getSkin();
        skin.setDirty(dirty);
    }

    public static <T> ObservableList<TableColumn<T, ?>> getVisibleLeafColumns(TableRow<T> tableRow) {
        TableRowSkin<T> skin = (TableRowSkin<T>) tableRow.getSkin();
        return skin.getVisibleLeafColumns();
    }

    public static <T> TableViewSkin<T> getTableViewSkin(TableRow<T> tableRow) {
        TableRowSkin<T> skin = (TableRowSkin<T>) tableRow.getSkin();
        return skin.getTableViewSkin();
    }

    public static <T> TreeTableViewSkin<T> getTableViewSkin(TreeTableRow<T> tableRow) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) tableRow.getSkin();
        return skin.getTableViewSkin();
    }

    public static <T> TreeItem<T> getTreeItem(TreeTableRow<T> tableRow) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) tableRow.getSkin();
        return skin.getTreeItem();
    }

    public static <T> ObjectProperty<Node> graphicProperty(TreeTableRow<T> tableRow) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) tableRow.getSkin();
        return skin.graphicProperty();
    }


    public static <T> VirtualFlow<?> getVirtualFlow(TableRow<T> table) {
        TableRowSkin<T> skin = (TableRowSkin<T>) table.getSkin();
        return skin.getVirtualFlow();
    }

    public static <T> VirtualFlow<?> getVirtualFlow(TreeTableRow<T> table) {
        TreeTableRowSkin<T> skin = (TreeTableRowSkin<T>) table.getSkin();
        return skin.getVirtualFlow();
    }

    public static List<IndexedCell<?>> getCells(TableRow tableRow) {
        TableRowSkin skin = (TableRowSkin) tableRow.getSkin();
        return skin.cells;
    }

    public static List<IndexedCell<?>> getCells(TreeTableRow<?> tableRow) {
        TreeTableRowSkin skin = (TreeTableRowSkin) tableRow.getSkin();
        return skin.cells;
    }



}

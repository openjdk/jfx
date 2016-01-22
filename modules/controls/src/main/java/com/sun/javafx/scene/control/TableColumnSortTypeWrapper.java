/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;

/**
 * Convenience class for dealing with sort types on TableColumn and TreeTableColumn,
 * although note that this will only work whilst TableColumn.SortType is
 * equivalent to TreeTableColumn.SortType. Once they diverge this code will need
 * to be replaced.
 */
public class TableColumnSortTypeWrapper {

    public static boolean isAscending(TableColumnBase<?, ?> column) {
        String sortTypeName = getSortTypeName(column);
        return "ASCENDING".equals(sortTypeName);
    }

    public static boolean isDescending(TableColumnBase<?, ?> column) {
        String sortTypeName = getSortTypeName(column);
        return "DESCENDING".equals(sortTypeName);
    }

    public static void setSortType(TableColumnBase<?,?> column, SortType sortType) {
        if (column instanceof TableColumn) {
            TableColumn tc = (TableColumn) column;
            tc.setSortType(sortType);
        } else if (column instanceof TreeTableColumn) {
            TreeTableColumn tc = (TreeTableColumn) column;
            if (sortType == SortType.ASCENDING) {
                tc.setSortType(javafx.scene.control.TreeTableColumn.SortType.ASCENDING);
            } else if (sortType == SortType.DESCENDING) {
                tc.setSortType(javafx.scene.control.TreeTableColumn.SortType.DESCENDING);
            } else if (sortType == null) {
                tc.setSortType(null);
            }
        }
    }

    public static String getSortTypeName(TableColumnBase<?,?> column) {
        if (column instanceof TableColumn) {
            TableColumn tc = (TableColumn) column;
            TableColumn.SortType st = tc.getSortType();
            return st == null ? null : st.name();
        } else if (column instanceof TreeTableColumn) {
            TreeTableColumn tc = (TreeTableColumn) column;
            TreeTableColumn.SortType st = tc.getSortType();
            return st == null ? null : st.name();
        }
        return null;
    }

    public static ObservableValue getSortTypeProperty(TableColumnBase<?,?> column) {
        if (column instanceof TableColumn) {
            return ((TableColumn) column).sortTypeProperty();
        } else if (column instanceof TreeTableColumn) {
            return ((TreeTableColumn) column).sortTypeProperty();
        }
        return null;
    }

}

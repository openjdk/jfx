/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;

public abstract class TableColumnComparatorBase<S,T> implements Comparator<S> {

    private final List<? extends TableColumnBase> columns;

    public TableColumnComparatorBase(TableColumnBase<S,T>... columns) {
        this(Arrays.asList(columns));
    }

    public TableColumnComparatorBase(List<? extends TableColumnBase> columns) {
        this.columns = new ArrayList<TableColumnBase>(columns);
    }

    public List<? extends TableColumnBase> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override public int compare(S o1, S o2) {
        for (TableColumnBase<S,T> tc : columns) {
            if (! isSortable(tc)) continue;

            T value1 = tc.getCellData(o1);
            T value2 = tc.getCellData(o2);

            int result = doCompare(tc, value1, value2);

            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    @Override public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.columns != null ? this.columns.hashCode() : 0);
        return hash;
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TableColumnComparatorBase other = (TableColumnComparatorBase) obj;
        if (this.columns != other.columns && (this.columns == null || !this.columns.equals(other.columns))) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "TableColumnComparatorBase [ columns: " + getColumns() + "] ";
    }

    public abstract boolean isSortable(final TableColumnBase<S,T> tcb);
    public abstract int doCompare(final TableColumnBase<S,T> tcb, final T value1, final T value2);





    public static final class TableColumnComparator<S,T> extends TableColumnComparatorBase<S, T> {

        public TableColumnComparator(TableColumn<S,T>... columns) {
            super(Arrays.asList(columns));
        }

        public TableColumnComparator(List<TableColumn<S,T>> columns) {
            super(columns);
        }

        @Override public boolean isSortable(final TableColumnBase<S,T> tcb) {
            TableColumn<S, T> tc = (TableColumn<S,T>)tcb;
            return tc.getSortType() != null && tc.isSortable();
        }

        @Override public int doCompare(final TableColumnBase<S,T> tcb, final T value1, final T value2) {
            TableColumn<S, T> tc = (TableColumn<S,T>)tcb;
            Comparator<T> c = tc.getComparator();
            switch (tc.getSortType()) {
                case ASCENDING: return c.compare(value1, value2);
                case DESCENDING: return c.compare(value2, value1);
            }
            return 0;
        }
    }


    public static final class TreeTableColumnComparator<S,T> extends TableColumnComparatorBase<S, T> {

        public TreeTableColumnComparator(TreeTableColumn<S,T>... columns) {
            super(Arrays.asList(columns));
        }

        public TreeTableColumnComparator(List<TreeTableColumn<S,T>> columns) {
            super(columns);
        }

        @Override public boolean isSortable(final TableColumnBase<S,T> tcb) {
            TreeTableColumn<S, T> tc = (TreeTableColumn<S,T>)tcb;
            return tc.getSortType() != null && tc.isSortable();
        }

        @Override public int doCompare(final TableColumnBase<S,T> tcb, final T value1, final T value2) {
            TreeTableColumn<S, T> tc = (TreeTableColumn<S,T>)tcb;
            Comparator<T> c = tc.getComparator();
            switch (tc.getSortType()) {
                case ASCENDING: return c.compare(value1, value2);
                case DESCENDING: return c.compare(value2, value1);
            }
            return 0;
        }
    }

}

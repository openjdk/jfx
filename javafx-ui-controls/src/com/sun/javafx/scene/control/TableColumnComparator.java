/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.scene.control.TableColumnBase;

public class TableColumnComparator<S,T> implements Comparator<S> {

    private final List<TableColumnBase<S,T>> columns;

    public TableColumnComparator(TableColumnBase<S,T>... columns) {
        this(Arrays.asList(columns));
    }
    
    public TableColumnComparator(List<TableColumnBase<S,T>> columns) {
        this.columns = new ArrayList<TableColumnBase<S, T>>(columns);
    }

    @ReturnsUnmodifiableCollection
    public List<TableColumnBase<S,T>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override public int compare(S o1, S o2) {
        for (TableColumnBase<S,T> tc : columns) {
            if (tc.getSortType() == null || ! tc.isSortable()) continue;
            
            Comparator<T> c = tc.getComparator();

            T value1 = tc.getCellData(o1);
            T value2 = tc.getCellData(o2);
            
            int result = 0;
            switch (tc.getSortType()) {
                case ASCENDING: result = c.compare(value1, value2); break;
                case DESCENDING: result = c.compare(value2, value1); break;    
            }
            
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
        final TableColumnComparator other = (TableColumnComparator) obj;
        if (this.columns != other.columns && (this.columns == null || !this.columns.equals(other.columns))) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "TableColumnComparator [ columns: " + getColumns() + "] ";
    }
}

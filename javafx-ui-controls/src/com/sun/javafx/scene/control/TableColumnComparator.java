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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.javafx.scene.control;

import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;

public class TableColumnComparator implements Comparator<Object> {

    private final ObservableList<TableColumnBase<?,?>> columns;

    public TableColumnComparator() {
        this.columns = FXCollections.observableArrayList();
    }

    public ObservableList<TableColumnBase<?,?>> getColumns() {
        return columns;
    }

    @Override public int compare(Object o1, Object o2) {
        for (TableColumnBase tc : columns) {
            Comparator c = tc.getComparator();

            Object value1 = tc.getCellData(o1);
            Object value2 = tc.getCellData(o2);
            
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
}

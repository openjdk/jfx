/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import javafx.scene.control.TableColumn;

public class TableColumnHeaderRetriever {

    public static int getColumnIndex(TableColumn col) {
        TableColumnHeader colHeader = VirtualFlowTestUtils.getTableColumnHeader(col.getTableView(), col);
        return colHeader == null ? -1 : colHeader.columnIndex;
    }

    public static int getColumnIndex(TableColumnHeader colHeader) {
        return colHeader == null ? -1 : colHeader.columnIndex;
    }

    public static void moveColumn(TableColumn col, int newPos) {
        TableColumnHeader colHeader = VirtualFlowTestUtils.getTableColumnHeader(col.getTableView(), col);
        colHeader.moveColumn(col, newPos);
    }

    public static int getSortPos(TableColumnHeader header) {
        return header.sortPos;
    }

    public static void moveColumn(TableColumn col, int dragOffset, int x) {
        TableColumnHeader colHeader = VirtualFlowTestUtils.getTableColumnHeader(col.getTableView(), col);
        colHeader.columnReorderingStarted(dragOffset);
        colHeader.columnReordering(x, 0);
        colHeader.columnReorderingComplete();
    }
}

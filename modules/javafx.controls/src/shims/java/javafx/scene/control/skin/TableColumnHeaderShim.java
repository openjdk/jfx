/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;

public class TableColumnHeaderShim {

    public static int getColumnIndex(TableColumnHeader colHeader) {
        return colHeader == null ? -1 : colHeader.columnIndex;
    }

    public static void moveColumn(TableColumn col, TableColumnHeader colHeader, int newPos) {
        colHeader.moveColumn(col, newPos);
    }

    public static int getSortPos(TableColumnHeader header) {
        return header.sortPos;
    }

    public static boolean getTableHeaderRowColumnDragLock(TableColumnHeader header) {
        return header.getTableHeaderRow().columnDragLock;
    }

    public static TableColumnHeader getColumnHeaderFor(TableHeaderRow header, final TableColumnBase<?,?> col) {
        return header.getColumnHeaderFor(col);
    }

    public static void columnReorderingStarted(TableColumnHeader header, double dragOffset) {
        header.columnReorderingStarted(dragOffset);
    }

    public static void columnReordering(TableColumnHeader header, double sceneX, double sceneY) {
        header.columnReordering(sceneX, sceneY);
    }

    public static void columnReorderingComplete(TableColumnHeader header) {
        header.columnReorderingComplete();
    }

    public static void resizeColumnToFitContent(TableColumnHeader header, int nbRows) {
        header.resizeColumnToFitContent(nbRows);
    }
}

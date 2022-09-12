/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import java.util.List;
import javafx.util.Callback;

/**
 * Implementation of constrained column resize algorithm that honors all Tree/TableColumn constraints -
 * minimum, preferred, and maximum width.
 *
 * @since 20
 */
public class ConstrainedColumnResize extends ConstrainedColumnResizeBase {
    public enum ResizeMode {
        AUTO_RESIZE_NEXT_COLUMN,
        AUTO_RESIZE_SUBSEQUENT_COLUMNS,
        AUTO_RESIZE_LAST_COLUMN,
        AUTO_RESIZE_ALL_COLUMNS
    }

    private final ResizeMode mode;

    public ConstrainedColumnResize(ResizeMode m) {
        this.mode = m;
    }

    @Override
    public boolean constrainedResize(ResizeFeaturesBase rf,
        List<? extends TableColumnBase<?,?>> visibleLeafColumns) {

        double contentWidth = rf.getContentWidth();
        if (contentWidth == 0.0) {
            return false;
        }

        ResizeHelper h = new ResizeHelper(rf, contentWidth, visibleLeafColumns, mode);
        h.resizeToContentWidth();

        boolean rv;
        TableColumnBase<?,?> column = rf.getColumn();
        if (column == null) {
            rv = false;
        } else {
            rv = h.resizeColumn(column);
        }

        h.applySizes();
        System.out.println(h.dump()); // FIX
        return rv;
    }

    public static TablePolicy forTable(ResizeMode m) {
        return new TablePolicy(m);
    }

    public static TreeTablePolicy forTreeTable(ResizeMode m) {
        return new TreeTablePolicy(m);
    }

    public static class TablePolicy
        extends ConstrainedColumnResize
        implements Callback<TableView.ResizeFeatures,Boolean> {

        public TablePolicy(ResizeMode m) {
            super(m);
        }

        @Override
        public Boolean call(TableView.ResizeFeatures rf) {
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = rf.getTable().getVisibleLeafColumns();
            return constrainedResize(rf, visibleLeafColumns);
        }
    }

    public static class TreeTablePolicy
        extends ConstrainedColumnResize
        implements Callback<TreeTableView.ResizeFeatures,Boolean> {

        public TreeTablePolicy(ResizeMode m) {
            super(m);
        }

        @Override
        public Boolean call(TreeTableView.ResizeFeatures rf) {
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = rf.getTable().getVisibleLeafColumns();
            return constrainedResize(rf, visibleLeafColumns);
        }
    }
}

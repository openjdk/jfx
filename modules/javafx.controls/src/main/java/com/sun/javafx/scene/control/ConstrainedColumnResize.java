/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

/**
 * A constrained column resize implementation that honors all Tree/TableColumn constraints -
 * minimum, preferred, and maximum width.
 */
public class ConstrainedColumnResize extends ConstrainedColumnResizeBase {

    public enum ResizeMode {
        AUTO_RESIZE_FLEX_HEAD,
        AUTO_RESIZE_FLEX_TAIL, // will be used to replace a (deprecated) CONSTRAINED_RESIZE_POLICY.
        AUTO_RESIZE_NEXT_COLUMN,
        AUTO_RESIZE_SUBSEQUENT_COLUMNS,
        AUTO_RESIZE_LAST_COLUMN,
        AUTO_RESIZE_ALL_COLUMNS
    }

    private final ResizeMode mode;

    public ConstrainedColumnResize(ResizeMode m) {
        this.mode = m;
    }

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

        // conditionally remove pseudoclass (given by this policy toString()) when the columns
        // are narrower than the available area and the rightmost column boundary line needs to be drawn.
        // hint: search modena.css for "constrained-resize" token
        Node n = rf.getTableControl();
        PseudoClass pc = PseudoClass.getPseudoClass(toString());
        boolean wide = h.applySizes();
        boolean current = n.getPseudoClassStates().contains(pc);
        if (wide != current) {
            if (wide) {
                n.pseudoClassStateChanged(pc, true);
            } else {
                n.pseudoClassStateChanged(pc, false);
            }
        }
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

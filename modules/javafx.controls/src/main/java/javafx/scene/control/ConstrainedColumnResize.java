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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

/**
 * Constrained columns resize algorithm which:
 * - honors minimal, preferred, and maximum widths
 * - unconditionally suppresses the horizontal scroll bar
 * 
 * @since 20
 */
public abstract class ConstrainedColumnResize {

    public enum ResizeMode {
        AUTO_RESIZE_NEXT_COLUMN,
        AUTO_RESIZE_SUBSEQUENT_COLUMNS,
        AUTO_RESIZE_LAST_COLUMN,
        AUTO_RESIZE_ALL_COLUMNS
    }

    protected static final double EPSILON = 0.0000001;

    public ConstrainedColumnResize() {
    }

//    public static TablePolicy forTable() {
//        return new TablePolicy();
//    }
//
//    public static TreeTablePolicy forTreeTable() {
//        return new TreeTablePolicy();
//    }
//
//    public static class TablePolicy
//        extends ConstrainedColumnResize
//        implements Callback<TableView.ResizeFeatures,Boolean> {
//
//        @Override
//        public Boolean call(TableView.ResizeFeatures f) {
//            List<? extends TableColumnBase<?,?>> visibleLeafColumns = f.getTable().getVisibleLeafColumns();
//            return constrainedResize(f, f.getContentWidth(), visibleLeafColumns);
//        }
//    }
//    
//    public static class TreeTablePolicy
//        extends ConstrainedColumnResize
//        implements Callback<TreeTableView.ResizeFeatures,Boolean> {
//        
//        @Override
//        public Boolean call(TreeTableView.ResizeFeatures f) {
//            List<? extends TableColumnBase<?,?>> visibleLeafColumns = f.getTable().getVisibleLeafColumns();
//            return constrainedResize(f, f.getContentWidth(), visibleLeafColumns);
//        }
//    }

    /**
     * The constrained resize algorithm used by TableView and TreeTableView.
     * TODO meaning of return?
     */
    public abstract boolean constrainedResize(ResizeFeaturesBase rf,
                                     double contentWidth,
                                     List<? extends TableColumnBase<?,?>> visibleLeafColumns);
    
    @Override
    public String toString() {
        // TODO rename
        return "new-constrained-resize";
    }
}

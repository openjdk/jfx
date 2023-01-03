/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import javafx.beans.NamedArg;

/**
 * This class is used to represent a single row/column/cell in a TreeTableView.
 * This is used throughout the TreeTableView API to represent which rows/columns/cells
 * are currently selected, focused, being edited, etc. Note that this class is
 * immutable once it is created.
 *
 * <p>Because the TreeTableView can have different
 * {@link SelectionMode selection modes}, the row and column properties in
 * TablePosition can be 'disabled' to represent an entire row or column. This is
 * done by setting the unrequired property to -1 or null.
 *
 * @param <S> The type of the {@link TreeItem} instances contained within the
 *      TreeTableView.
 * @param <T> The type of the items contained within the TreeTableColumn.
 * @see TreeTableView
 * @see TreeTableColumn
 * @since JavaFX 8.0
 */
public class TreeTablePosition<S,T> extends TablePositionBase<TreeTableColumn<S,T>> {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a TreeTablePosition instance to represent the given row/column
     * position in the given TreeTableView instance. Both the TreeTableView and
     * TreeTableColumn are referenced weakly in this class, so it is possible that
     * they will be null when their respective getters are called.
     *
     * @param treeTableView The TreeTableView that this position is related to.
     * @param row The row that this TreeTablePosition is representing.
     * @param tableColumn The TreeTableColumn instance that this TreeTablePosition represents.
     */
    public TreeTablePosition(@NamedArg("treeTableView") TreeTableView<S> treeTableView, @NamedArg("row") int row, @NamedArg("tableColumn") TreeTableColumn<S,T> tableColumn) {
        this(treeTableView, row, tableColumn, true);
    }

    // Not public API
    TreeTablePosition(@NamedArg("treeTableView") TreeTableView<S> treeTableView, @NamedArg("row") int row, @NamedArg("tableColumn") TreeTableColumn<S,T> tableColumn, boolean doLookup) {
        super(row, tableColumn);
        this.controlRef = new WeakReference<>(treeTableView);
        this.treeItemRef = new WeakReference<>(doLookup ?
                (treeTableView != null ? treeTableView.getTreeItem(row) : null) : null);

        nonFixedColumnIndex = treeTableView == null || tableColumn == null ? -1 : treeTableView.getVisibleLeafIndex(tableColumn);
    }

    // Not public API. A Copy-like constructor with a different row.
    // It is used for updating the selection when the TreeItems are
    // sorted using TreeTableView.sort() or reordered using setAll().
    TreeTablePosition(@NamedArg("treeTableView") TreeTablePosition<S, T> pos, @NamedArg("row") int row) {
        super(row, pos.getTableColumn());
        this.controlRef = new WeakReference<>(pos.getTreeTableView());
        this.treeItemRef = new WeakReference<>(pos.getTreeItem());
        nonFixedColumnIndex = pos.getColumn();
    }



    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final WeakReference<TreeTableView<S>> controlRef;
    private final WeakReference<TreeItem<S>> treeItemRef;
    int fixedColumnIndex = -1;
    private final int nonFixedColumnIndex;

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The column index that this TreeTablePosition represents in the TreeTableView. It
     * is -1 if the TreeTableView or TreeTableColumn instances are null at the time the class
     * is instantiated (i.e. it is computed at construction).
     */
    @Override public int getColumn() {
        if (fixedColumnIndex > -1) {
            return fixedColumnIndex;
        }

        return nonFixedColumnIndex;
    }

    /**
     * The TreeTableView that this TreeTablePosition is related to.
     * @return the TreeTableView that this TreeTablePosition is related to
     */
    public final TreeTableView<S> getTreeTableView() {
        return controlRef.get();
    }

    @Override public final TreeTableColumn<S,T> getTableColumn() {
        // Forcing the return type to be TreeTableColumn<S,T>, not TableColumnBase<S,T>
        return super.getTableColumn();
    }

    /**
     * Returns the {@link TreeItem} that backs the {@link #getRow()} row}.
     * @return the {@link TreeItem} that backs the {@link #getRow()} row}
     */
    public final TreeItem<S> getTreeItem() {
        return treeItemRef.get();
    }
}

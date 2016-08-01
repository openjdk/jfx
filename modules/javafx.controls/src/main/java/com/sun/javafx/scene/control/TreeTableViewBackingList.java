/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.NonIterableChange;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.util.List;

/**
 * A simple read only list structure that maps into the TreeTableView tree
 * structure.
 */
public class TreeTableViewBackingList<T> extends ReadOnlyUnbackedObservableList<TreeItem<T>> {
    private final TreeTableView<T> treeTable;

    private int size = -1;

    public TreeTableViewBackingList(TreeTableView<T> treeTable) {
        this.treeTable = treeTable;
    }

    public void resetSize() {
        int oldSize = size;
        size = -1;

        // TODO we can certainly make this better....but it may not really matter
        callObservers(new NonIterableChange.GenericAddRemoveChange<>(0, oldSize, FXCollections.<TreeItem<T>>emptyObservableList(), this));
    }

    @Override public TreeItem<T> get(int i) {
        return treeTable.getTreeItem(i);
    }

    @Override public int size() {
        if (size == -1) {
            size = treeTable.getExpandedItemCount();
        }
        return size;
    }
}
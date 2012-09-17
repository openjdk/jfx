/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.preview.javafx.scene.control.TreeTableRow;
import com.preview.javafx.scene.control.TreeTableView;
import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import com.sun.javafx.scene.control.behavior.TreeTableViewBehavior;
import java.lang.ref.WeakReference;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

/**
 *
 */
public class TreeTableViewSkin<T> extends SkinBase<TreeTableView<T>, TreeTableViewBehavior<T>> {
    
    private final TableView<TreeItem<T>> table;
    private final TreeTableViewBackingList<T> tableBackingList;
    
    private WeakReference<TreeItem> weakRootRef;

    public TreeTableViewSkin(final TreeTableView<T> control) {
        super(control, new TreeTableViewBehavior<T>(control));
        
        table = new TableView<TreeItem<T>>();
        
        tableBackingList = new TreeTableViewBackingList(control);
        table.setItems(tableBackingList);
        
        // get defined columns and set up listener so that the embedded TableView
        // always has the same columns as what is in the TreeTableView columns list
        updateColumns();
        control.getColumns().addListener(new ListChangeListener<TableColumn<TreeItem<T>, ?>>() {
            @Override public void onChanged(Change<? extends TableColumn<TreeItem<T>, ?>> change) {
                updateColumns();
            }
        });
        
        // install custom row factory to handle indentation
        table.setRowFactory(new Callback<TableView<TreeItem<T>>, TableRow<TreeItem<T>>>() {
            @Override public TreeTableRow<TreeItem<T>> call(TableView<TreeItem<T>> p) {
                TreeTableRow row = new TreeTableRow<TreeItem<T>>();
                
                if (row.getDisclosureNode() == null) {
                    final StackPane disclosureNode = new StackPane();
                    disclosureNode.getStyleClass().setAll("tree-disclosure-node");

                    final StackPane disclosureNodeArrow = new StackPane();
                    disclosureNodeArrow.getStyleClass().setAll("arrow");
                    disclosureNode.getChildren().add(disclosureNodeArrow);

                    row.setDisclosureNode(disclosureNode);
                }
                
                row.updateTreeTableView(control);
                row.getStyleClass().add("tree-table-row");
                return row;
            }
        });
        
        getChildren().add(table);
        
        registerChangeListener(control.rootProperty(), "ROOT");
        registerChangeListener(control.showRootProperty(), "SHOW_ROOT");
        registerChangeListener(control.cellFactoryProperty(), "CELL_FACTORY");
        registerChangeListener(control.impl_treeItemCountProperty(), "TREE_ITEM_COUNT");
        
        updateItemCount();
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if ("ROOT".equals(p)) {
            setRoot(getSkinnable().getRoot());
        } else if ("SHOW_ROOT".equals(p)) {
            // if we turn off showing the root, then we must ensure the root
            // is expanded - otherwise we end up with no visible items in
            // the tree.
            if (! getSkinnable().isShowRoot() && getRoot() != null) {
                 getRoot().setExpanded(true);

                 // update the item count in the flow and behavior instances
                updateItemCount();
            }
        } else if ("CELL_FACTORY".equals(p)) {
            // FIXME can't set treeview cell factory in to a table!
        } else if ("TREE_ITEM_COUNT".equals(p)) {
            updateItemCount();
        }
    }
    
    private TreeItem getRoot() {
        return weakRootRef == null ? null : weakRootRef.get();
    }
    
    private void setRoot(TreeItem newRoot) {
        weakRootRef = new WeakReference<TreeItem>(newRoot);
    }

    private void updateColumns() {
        table.getColumns().setAll(getSkinnable().getColumns());
    }
    
    private void updateItemCount() {
        tableBackingList.resetSize();
    }

    private static class TreeTableViewBackingList<T> extends ReadOnlyUnbackedObservableList<TreeItem<T>> {
        private final TreeTableView<T> treeTable;
        
        private int size = -1;
        
        TreeTableViewBackingList(TreeTableView<T> treeTable) {
            this.treeTable = treeTable;
        }
        
        void resetSize() {
            int oldSize = size;
            size = -1;
            
            // TODO we can certainly make this better....but it may not really matter
            callObservers(new NonIterableChange.GenericAddRemoveChange<TreeItem<T>>(
                    0, oldSize, FXCollections.<TreeItem<T>>emptyObservableList(), this));
        }
        
        @Override public TreeItem<T> get(int i) {
            return treeTable.getTreeItem(i);
        }

        @Override public int size() {
            if (size == -1) {
                size = treeTable.impl_getTreeItemCount();
            }
            return size;
        }
    }
}

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
package com.sun.javafx.scene.control.behavior;

import com.preview.javafx.scene.control.TreeTableView;
import com.preview.javafx.scene.control.TreeTableRow;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 *
 */
public class TreeTableRowBehavior<T> extends CellBehaviorBase<TreeTableRow<T>> {

    public TreeTableRowBehavior(TreeTableRow<T> control) {
        super(control);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        
//        if (e.getClickCount() == 2) {
//            ObservableList<T> items = getControl().getTableView().getItems();
//            TreeItem<File> treeItem = items.get(getControl.getIndex());
//            treeItem.setExpanded(! treeItem.isExpanded());
//        }
        
        TreeTableView<T> tv = getControl().getTreeTableView();
        TreeItem treeItem = getControl().getTreeItem();
        int index = getControl().getIndex();
        MultipleSelectionModel sm = tv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        tv.getSelectionModel().clearAndSelect(index);

        // handle editing, which only occurs with the primary mouse button
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() == 1 && isAlreadySelected) {
                tv.edit(treeItem);
            } else if (e.getClickCount() == 1) {
                // cancel editing
                tv.edit(null);
            } else if (e.getClickCount() == 2/* && ! getControl().isEditable()*/) {
                if (treeItem.isLeaf()) {
                    // attempt to edit
                    tv.edit(treeItem);
                } else {
                    // try to expand/collapse branch tree item
                    treeItem.setExpanded(! treeItem.isExpanded());
                }
            }
        }
    }
}

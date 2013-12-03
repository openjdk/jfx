/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview;

import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview.HierarchyTreeTableCell.Column;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview.HierarchyTreeTableCell.HIERARCHY_TREE_TABLE_CELL;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview.HierarchyTreeTableRow.HIERARCHY_TREE_TABLE_ROW;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;

/**
 *
 * p
 */
public abstract class HierarchyTreeTableViewUtils {

    /**
     * Returns the TreeTableRows for the specified TreeTableView.
     *
     * @param <T>
     * @param treeTableView the TreeTableView owner
     * @return the TreeTableRows for the specified TreeTableView.
     */
    public static <T> Set<Node> getTreeTableRows(final TreeTableView<T> treeTableView) {
        assert treeTableView != null;
        // Looks for the sub nodes which match the CSS selector
        return treeTableView.lookupAll("." + HIERARCHY_TREE_TABLE_ROW); //NOI18N
    }

    /**
     * Returns the TreeTableRow object corresponding to the specified TreeItem.
     *
     * @param treeTableView the TreeTableView owner
     * @param treeItem the TreeItem instance
     * @return the TreeTableRow object corresponding to the specified TreeItem.
     */
    public static TreeTableRow<?> getTreeTableRow(final TreeTableView<?> treeTableView, final TreeItem<?> treeItem) {
        return getTreeTableRow(getTreeTableRows(treeTableView), treeItem);
    }

    /**
     * Returns the TreeTableRow object corresponding to the specified index.
     *
     * @param treeTableView the TreeTableView owner
     * @param index the TreeTableRow index
     * @return the TreeTableRow object corresponding to the specified index.
     */
    public static TreeTableRow<?> getTreeTableRow(final TreeTableView<?> treeTableView, final int index) {
        return getTreeTableRow(getTreeTableRows(treeTableView), index);
    }

    /**
     * Returns the TreeTableRow object corresponding to the specified TreeItem.
     *
     * @param treeTableRows the set of TreeTableRows
     * @param treeItem the TreeItem instance
     * @return the TreeTableRow object corresponding to the specified TreeItem.
     */
    public static TreeTableRow<?> getTreeTableRow(final Set<Node> treeTableRows, final TreeItem<?> treeItem) {
        assert treeTableRows != null;
        assert treeItem != null;
        for (Node node : treeTableRows) {
            assert node instanceof TreeTableRow;
            final TreeTableRow<?> treetableRow = (TreeTableRow) node;
            if (treeItem.getValue() != null
                    && treeItem.getValue().equals(treetableRow.getItem())) {
                return treetableRow;
            }
        }
        return null;
    }

    /**
     * Returns the TreeTableRow object corresponding to the specified index.
     *
     * @param treeTableRows the set of TreeTableRows
     * @param index the TreeTableRow index
     * @return the TreeTableRow object corresponding to the specified index.
     */
    public static TreeTableRow<?> getTreeTableRow(final Set<Node> treeTableRows, final int index) {
        assert treeTableRows != null;
        for (Node node : treeTableRows) {
            assert node instanceof TreeTableRow;
            final TreeTableRow<?> treetableRow = (TreeTableRow) node;
            if (treetableRow.getIndex() == index) {
                return treetableRow;
            }
        }
        return null;
    }

    /**
     * Returns the TreeTableCells for the specified TreeTableView.
     *
     * @param <T>
     * @param treeTableView the TreeTableView owner
     * @return the TreeTableCells for the specified TreeTableView.
     */
    public static <T> Set<Node> getTreeTableCells(final TreeTableView<T> treeTableView) {
        assert treeTableView != null;
        // Looks for the sub nodes which match the CSS selector
        return treeTableView.lookupAll("." + HIERARCHY_TREE_TABLE_CELL); //NOI18N
    }

    /**
     * Returns the TreeTableCell object corresponding to the specified TreeItem.
     *
     * @param treeTableView the TreeTableView owner
     * @param column the column owner
     * @param treeItem the TreeItem instance
     * @return the TreeTableCell object corresponding to the specified TreeItem.
     */
    public static TreeTableCell<?, ?> getTreeTableCell(final TreeTableView<?> treeTableView, final Column column, final TreeItem<?> treeItem) {
        return getTreeTableCell(getTreeTableCells(treeTableView), column, treeItem);
    }

    /**
     * Returns the TreeTableCell object corresponding to the specified TreeItem.
     *
     * @param treeTableCells the set of TreeTableCells
     * @param column the column owner
     * @param treeItem the TreeItem instance
     * @return the TreeTableCell object corresponding to the specified TreeItem.
     */
    public static TreeTableCell<?, ?> getTreeTableCell(final Set<Node> treeTableCells, final Column column, final TreeItem<?> treeItem) {
        assert treeTableCells != null;
        for (Node node : treeTableCells) {
            assert node instanceof HierarchyTreeTableCell;
            final HierarchyTreeTableCell<?, ?> treeTableCell = (HierarchyTreeTableCell) node;
            // Return the TreeTableCell belonging to the specified column
            assert column != null;
            if (column == treeTableCell.getColumn()) {
                final TreeItem<?> ti = treeTableCell.getTreeTableRow().getTreeItem();
                if (treeItem != null && treeItem.equals(ti)) {
                    return treeTableCell;
                }
            }
        }
        return null;
    }

    /**
     * Return true if the specified child TreeItem can be reparented to the
     * specified parent TreeItem. This means that the child TreeItem object is
     * not in the parent chain of the parent TreeItem.
     *
     * @param <T>
     * @param child
     * @param parent
     * @return
     */
    public static <T> boolean canReparentTreeItem(final TreeItem<T> child, TreeItem<T> parent) {
        if (child == parent) {
            return false;
        }
        int childLevel = TreeTableView.getNodeLevel(child);
        int parentLevel = TreeTableView.getNodeLevel(parent);
        while (parentLevel >= childLevel) {
            if (parent == child) {
                return false;
            }
            parent = parent.getParent();
            parentLevel--;
        }
        return true;
    }

    /**
     * Return the common parent TreeItem of the specified TreeItems.
     *
     * @param <T>
     * @param treeItems
     * @return
     */
    public static <T> TreeItem<T> getCommonParentTreeItem(
            final List<TreeItem<T>> treeItems) {

        assert treeItems != null && !treeItems.isEmpty();

        // TreeItems contains ROOT 
        // => return ROOT as the common parent
        for (TreeItem<T> treeItem : treeItems) {
            if (TreeTableView.getNodeLevel(treeItem) == 0) {
                return treeItem;
            }
        }

        // TreeItem single selection
        // => the common parent is the single TreeItem parent
        if (treeItems.size() == 1) {
            return treeItems.get(0).getParent();
        } //
        // TreeItem multi selection
        else {
            assert treeItems.size() >= 2;
            TreeItem<T> parent = null;
            TreeItem<T> child = treeItems.get(0);
            for (int index = 1; index < treeItems.size(); index++) {
                parent = getCommonParentTreeItem(child, treeItems.get(index));
                // We reached the ROOT level
                // => common parent is ROOT TreeItem
                if (TreeTableView.getNodeLevel(parent) == 0) {
                    break;
                } else {
                    child = parent;
                }
            }
            return parent;
        }
    }

    private static <T> TreeItem<T> getCommonParentTreeItem(
            final TreeItem<T> child1,
            final TreeItem<T> child2) {

        assert child1 != null && child2 != null;

        int child1Level = TreeTableView.getNodeLevel(child1);
        int child2Level = TreeTableView.getNodeLevel(child2);
        // Neither child1 nor child2 is ROOT TreeItem
        assert child1Level > 0 && child2Level > 0;

        TreeItem<T> parent1 = child1.getParent();
        TreeItem<T> parent2 = child2.getParent();

        if (child1Level < child2Level) {
            while (child1Level < child2Level) {
                parent2 = parent2.getParent();
                child2Level--;
            }
            // We reached the common parent TreeItem
            if (parent1 == parent2) {
                return parent1;
            } else {
                // At this step, parent1 and parent2 have same node level 
                // within the TreeView
                while (parent1 != parent2) {
                    parent1 = parent1.getParent();
                    parent2 = parent2.getParent();
                }
                return parent1;
            }
        } else {
            while (child1Level > child2Level) {
                parent1 = parent1.getParent();
                child1Level--;
            }
            // We reached the common parent TreeItem
            if (parent1 == parent2) {
                return parent1;
            } else {
                // At this step, parent1 and parent2 have same node level 
                // within the TreeView
                while (parent1 != parent2) {
                    parent1 = parent1.getParent();
                    parent2 = parent2.getParent();
                }
                return parent1;
            }
        }
    }
}

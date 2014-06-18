/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treeview;

import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treeview.HierarchyTreeCell.HIERARCHY_TREE_CELL;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 *
 * p
 */
public abstract class HierarchyTreeViewUtils {

    /**
     * Returns the TreeCells for the specified TreeView.
     *
     * @param <T>
     * @param treeView the TreeView owner
     * @return the TreeCells for the specified TreeView.
     */
    public static <T> Set<Node> getTreeCells(final TreeView<T> treeView) {
        assert treeView != null;
        // Looks for the sub nodes which match the CSS selector
        return treeView.lookupAll("." + HIERARCHY_TREE_CELL); //NOI18N
    }

    /**
     * Returns the TreeCell object corresponding to the specified TreeItem.
     *
     * @param treeView the TreeView owner
     * @param treeItem the TreeItem instance
     * @return the TreeCell object corresponding to the specified TreeItem.
     */
    public static TreeCell<?> getTreeCell(final TreeView<?> treeView, final TreeItem<?> treeItem) {
        return getTreeCell(getTreeCells(treeView), treeItem);
    }

    /**
     * Returns the TreeCell object corresponding to the specified index.
     *
     * @param treeView the TreeView owner
     * @param index the TreeCell index
     * @return the TreeCell object corresponding to the specified index.
     */
    public static TreeCell<?> getTreeCell(final TreeView<?> treeView, final int index) {
        return getTreeCell(getTreeCells(treeView), index);
    }

    /**
     * Returns the TreeCell object corresponding to the specified TreeItem.
     *
     * @param treeCells the set of TreeCells
     * @param treeItem the TreeItem instance
     * @return the TreeCell object corresponding to the specified TreeItem.
     */
    public static TreeCell<?> getTreeCell(final Set<Node> treeCells, final TreeItem<?> treeItem) {
        assert treeCells != null;
        assert treeItem != null;
        for (Node node : treeCells) {
            assert node instanceof TreeCell;
            final TreeCell<?> treeCell = (TreeCell<?>) node;
            if (treeItem.getValue() != null
                    && treeItem.getValue().equals(treeCell.getItem())) {
                return treeCell;
            }
        }
        return null;
    }

    /**
     * Returns the TreeCell object corresponding to the specified index.
     *
     * @param treeCells the set of TreeCells
     * @param index the TreeCell index
     * @return the TreeCell object corresponding to the specified index.
     */
    public static TreeCell<?> getTreeCell(final Set<Node> treeCells, final int index) {
        assert treeCells != null;
        for (Node node : treeCells) {
            assert node instanceof TreeCell;
            final TreeCell<?> treeCell = (TreeCell<?>) node;
            if (treeCell.getIndex() == index) {
                return treeCell;
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
        int childLevel = Deprecation.getNodeLevel(child);
        int parentLevel = Deprecation.getNodeLevel(parent);
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
            if (Deprecation.getNodeLevel(treeItem) == 0) {
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
                if (Deprecation.getNodeLevel(parent) == 0) {
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

        int child1Level = Deprecation.getNodeLevel(child1);
        int child2Level = Deprecation.getNodeLevel(child2);
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

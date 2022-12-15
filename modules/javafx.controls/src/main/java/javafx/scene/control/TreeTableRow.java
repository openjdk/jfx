/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.PseudoClass;
import javafx.scene.control.skin.TreeTableRowSkin;
import java.lang.ref.WeakReference;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.TreeTableView.TreeTableViewFocusModel;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;

/**
 * <p>TreeTableRow is an {@link javafx.scene.control.IndexedCell IndexedCell}, but
 * rarely needs to be used by developers creating TreeTableView instances. The only
 * time TreeTableRow is likely to be encountered at all by a developer is if they
 * wish to create a custom {@link TreeTableView#rowFactoryProperty() rowFactory}
 * that replaces an entire row of a TreeTableView.</p>
 *
 * <p>More often than not, it is actually easier for a developer to customize
 * individual cells in a row, rather than the whole row itself. To do this,
 * you can specify a custom {@link TreeTableColumn#cellFactoryProperty() cellFactory}
 * on each TreeTableColumn instance.</p>
 *
 * @see TreeTableView
 * @see TreeTableColumn
 * @see TreeTableCell
 * @see IndexedCell
 * @see Cell
 * @param <T> The type of the item contained within the Cell.
 * @since JavaFX 8.0
 */
public class TreeTableRow<T> extends IndexedCell<T> {


    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TreeTableRow instance.
     */
    public TreeTableRow() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.TREE_TABLE_ROW);
    }



    /* *************************************************************************
     *                                                                         *
     * Callbacks and events                                                    *
     *                                                                         *
     **************************************************************************/

    private final ListChangeListener<Integer> selectedListener = c -> {
        updateSelection();
    };

    private final InvalidationListener focusedListener = valueModel -> {
        updateFocus();
    };

    private final InvalidationListener editingListener = valueModel -> {
        updateEditing();
    };

    private final InvalidationListener leafListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            // necessary to update the disclosure node in the skin when the
            // leaf property changes
            TreeItem<T> treeItem = getTreeItem();
            if (treeItem != null) {
                requestLayout();
            }
        }
    };

    private boolean oldExpanded;
    private final InvalidationListener treeItemExpandedInvalidationListener = o -> {
        final boolean expanded = ((BooleanProperty)o).get();
        pseudoClassStateChanged(EXPANDED_PSEUDOCLASS_STATE,   expanded);
        pseudoClassStateChanged(COLLAPSED_PSEUDOCLASS_STATE, !expanded);
        if (expanded != oldExpanded) {
            notifyAccessibleAttributeChanged(AccessibleAttribute.EXPANDED);
        }
        oldExpanded = expanded;
    };

    private final WeakListChangeListener<Integer> weakSelectedListener =
            new WeakListChangeListener<>(selectedListener);
    private final WeakInvalidationListener weakFocusedListener =
            new WeakInvalidationListener(focusedListener);
    private final WeakInvalidationListener weakEditingListener =
            new WeakInvalidationListener(editingListener);
    private final WeakInvalidationListener weakLeafListener =
            new WeakInvalidationListener(leafListener);
    private final WeakInvalidationListener weakTreeItemExpandedInvalidationListener =
            new WeakInvalidationListener(treeItemExpandedInvalidationListener);



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- TreeItem
    private ReadOnlyObjectWrapper<TreeItem<T>> treeItem =
        new ReadOnlyObjectWrapper<>(this, "treeItem") {

            TreeItem<T> oldValue = null;

            @Override protected void invalidated() {
                if (oldValue != null) {
                    oldValue.expandedProperty().removeListener(weakTreeItemExpandedInvalidationListener);
                }

                oldValue = get();

                if (oldValue != null) {
                    oldExpanded = oldValue.isExpanded();
                    oldValue.expandedProperty().addListener(weakTreeItemExpandedInvalidationListener);
                    // fake an invalidation to ensure updated pseudo-class state
                    weakTreeItemExpandedInvalidationListener.invalidated(oldValue.expandedProperty());
                }
            }
    };
    private void setTreeItem(TreeItem<T> value) {
        treeItem.set(value);
    }

    /**
     * Returns the TreeItem currently set in this TreeTableRow.
     * @return the TreeItem currently set in this TreeTableRow
     */
    public final TreeItem<T> getTreeItem() { return treeItem.get(); }

    /**
     * Each TreeTableCell represents at most a single {@link TreeItem}, which is
     * represented by this property.
     * @return the tree item property
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> treeItemProperty() { return treeItem.getReadOnlyProperty(); }



    // --- Disclosure Node
    private ObjectProperty<Node> disclosureNode = new SimpleObjectProperty<>(this, "disclosureNode");

    /**
     * The node to use as the "disclosure" triangle, or toggle, used for
     * expanding and collapsing items. This is only used in the case of
     * an item in the tree which contains child items. If not specified, the
     * TreeTableCell's Skin implementation is responsible for providing a default
     * disclosure node.
     * @param value the disclosure node
     */
    public final void setDisclosureNode(Node value) { disclosureNodeProperty().set(value); }

    /**
     * Returns the current disclosure node set in this TreeTableCell.
     * @return the disclosure node
     */
    public final Node getDisclosureNode() { return disclosureNode.get(); }

    /**
     * The disclosure node is commonly seen represented as a triangle that rotates
     * on screen to indicate whether or not the TreeItem that it is placed
     * beside is expanded or collapsed.
     * @return the disclosure node property
     */
    public final ObjectProperty<Node> disclosureNodeProperty() { return disclosureNode; }


    // --- TreeView
    private ReadOnlyObjectWrapper<TreeTableView<T>> treeTableView = new ReadOnlyObjectWrapper<>(this, "treeTableView") {
        private WeakReference<TreeTableView<T>> weakTreeTableViewRef;
        @Override protected void invalidated() {
            TreeTableViewSelectionModel<T> sm;
            TreeTableViewFocusModel<T> fm;

            if (weakTreeTableViewRef != null) {
                TreeTableView<T> oldTreeTableView = weakTreeTableViewRef.get();
                if (oldTreeTableView != null) {
                    // remove old listeners
                    sm = oldTreeTableView.getSelectionModel();
                    if (sm != null) {
                        sm.getSelectedIndices().removeListener(weakSelectedListener);
                    }

                    fm = oldTreeTableView.getFocusModel();
                    if (fm != null) {
                        fm.focusedIndexProperty().removeListener(weakFocusedListener);
                    }

                    oldTreeTableView.editingCellProperty().removeListener(weakEditingListener);
                }

                weakTreeTableViewRef = null;
            }

            if (get() != null) {
                sm = get().getSelectionModel();
                if (sm != null) {
                    // listening for changes to treeView.selectedIndex and IndexedCell.index,
                    // to determine if this cell is selected
                    sm.getSelectedIndices().addListener(weakSelectedListener);
                }

                fm = get().getFocusModel();
                if (fm != null) {
                    // similar to above, but this time for focus
                    fm.focusedIndexProperty().addListener(weakFocusedListener);
                }

                get().editingCellProperty().addListener(weakEditingListener);

                weakTreeTableViewRef = new WeakReference<>(get());
            }

            updateItem();
            requestLayout();
        }
    };

    private void setTreeTableView(TreeTableView<T> value) { treeTableView.set(value); }

    /**
     * Returns the TreeTableView associated with this TreeTableCell.
     * @return the tree table view
     */
    public final TreeTableView<T> getTreeTableView() { return treeTableView.get(); }

    /**
     * A TreeTableCell is explicitly linked to a single {@link TreeTableView} instance,
     * which is represented by this property.
     * @return the tree table view property
     */
    public final ReadOnlyObjectProperty<TreeTableView<T>> treeTableViewProperty() { return treeTableView.getReadOnlyProperty(); }




    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     *************************************************************************
     * @param oldIndex
     * @param newIndex*/


    @Override void indexChanged(int oldIndex, int newIndex) {
        index = getIndex();

        // when the cell index changes, this may result in the cell
        // changing state to be selected and/or focused.
        updateItem();
        updateSelection();
        updateFocus();
//        oldIndex = index;
    }


    /** {@inheritDoc} */
    @Override public void startEdit() {
        final TreeTableView<T> treeTable = getTreeTableView();
        if (! isEditable() || (treeTable != null && ! treeTable.isEditable())) {
            return;
        }

        // it makes sense to get the cell into its editing state before firing
        // the event to the TreeView below, so that's what we're doing here
        // by calling super.startEdit().
        super.startEdit();

         // Inform the TreeView of the edit starting.
        if (treeTable != null) {
            treeTable.fireEvent(new TreeTableView.EditEvent<>(treeTable,
                    TreeTableView.<T>editStartEvent(),
                    getTreeItem(),
                    getItem(),
                    null));

            treeTable.requestFocus();
        }
    }

     /** {@inheritDoc} */
    @Override public void commitEdit(T newValue) {
        if (! isEditing()) return;
        final TreeItem<T> treeItem = getTreeItem();
        final TreeTableView<T> treeTable = getTreeTableView();
        if (treeTable != null) {
            // Inform the TreeView of the edit being ready to be committed.
            treeTable.fireEvent(new TreeTableView.EditEvent<>(treeTable,
                    TreeTableView.<T>editCommitEvent(),
                    treeItem,
                    getItem(),
                    newValue));
        }

        // update the item within this cell, so that it represents the new value
        if (treeItem != null) {
            treeItem.setValue(newValue);
            updateTreeItem(treeItem);
            updateItem(newValue, false);
        }

        // inform parent classes of the commit, so that they can switch us
        // out of the editing state
        super.commitEdit(newValue);

        if (treeTable != null) {
            // reset the editing item in the TreetView
            treeTable.edit(-1, null);
            treeTable.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;

        TreeTableView<T> treeTable = getTreeTableView();
        if (treeTable != null) {
            treeTable.fireEvent(new TreeTableView.EditEvent<>(treeTable,
                    TreeTableView.<T>editCancelEvent(),
                    getTreeItem(),
                    getItem(),
                    null));
        }

        super.cancelEdit();

        if (treeTable != null) {
            // reset the editing index on the TreeView
            treeTable.edit(-1, null);
            treeTable.requestFocus();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private int index = -1;
    private boolean isFirstRun = true;

    private void updateItem() {
        TreeTableView<T> tv = getTreeTableView();
        if (tv == null) return;

        // Compute whether the index for this cell is for a real item
        boolean valid = index >=0 && index < tv.getExpandedItemCount();

        final TreeItem<T> oldTreeItem = getTreeItem();
        final boolean isEmpty = isEmpty();

        // Cause the cell to update itself
        if (valid) {
            // update the TreeCell state.
            // get the new treeItem that is about to go in to the TreeCell
            final TreeItem<T> newTreeItem = tv.getTreeItem(index);
            final T newValue = newTreeItem == null ? null : newTreeItem.getValue();

            // For the sake of RT-14279, it is important that the order of these
            // method calls is as shown below. If the order is switched, it is
            // likely that events will be fired where the item is null, even
            // though calling cell.getTreeItem().getValue() returns the value
            // as expected

            // There used to be conditional code here to prevent updateItem from
            // being called when the value didn't change, but that led us to
            // issues such as RT-33108, where the value didn't change but the item
            // we needed to be listening to did. Without calling updateItem we
            // were breaking things, so once again the conditionals are gone.
            updateTreeItem(newTreeItem);
            updateItem(newValue, false);
        } else {
            // RT-30484 We need to allow a first run to be special-cased to allow
            // for the updateItem method to be called at least once to allow for
            // the correct visual state to be set up. In particular, in RT-30484
            // refer to Ensemble8PopUpTree.png - in this case the arrows are being
            // shown as the new cells are instantiated with the arrows in the
            // children list, and are only hidden in updateItem.
            if ((!isEmpty && oldTreeItem != null) || isFirstRun) {
                updateTreeItem(null);
                updateItem(null, true);
                isFirstRun = false;
            }
        }
    }

    private void updateSelection() {
        if (isEmpty()) return;
        if (index == -1 || getTreeTableView() == null) return;

        TreeTableViewSelectionModel<T> sm = getTreeTableView().getSelectionModel();
        if (sm == null) {
            if (isSelected()) {
                updateSelected(false);
            }
            return;
        }

        boolean isSelected = !sm.isCellSelectionEnabled() && sm.isSelected(index);
        if (isSelected() != isSelected) {
            updateSelected(isSelected);
        }
    }

    private void updateFocus() {
        if (getIndex() == -1 || getTreeTableView() == null) return;
        if (getTreeTableView().getFocusModel() == null) return;

        setFocused(getTreeTableView().getFocusModel().isFocused(getIndex()));
    }

    private void updateEditing() {
        if (getIndex() == -1 || getTreeTableView() == null || getTreeItem() == null) return;

        final TreeTablePosition<T,?> editingCell = getTreeTableView().getEditingCell();
        if (editingCell != null && editingCell.getTableColumn() != null) {
            return;
        }

        final TreeItem<T> editItem = editingCell == null ? null : editingCell.getTreeItem();
        if (! isEditing() && getTreeItem().equals(editItem)) {
            startEdit();
        } else if (isEditing() && ! getTreeItem().equals(editItem)) {
            cancelEdit();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Expert API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Updates the TreeTableView associated with this TreeTableCell.
     *
     * @param treeTable The new TreeTableView that should be associated with this
     *         TreeTableCell.
     * Note: This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTreeTableView(TreeTableView<T> treeTable) {
        setTreeTableView(treeTable);
    }

    /**
     * Updates the TreeItem associated with this TreeTableCell.
     *
     * @param treeItem The new TreeItem that should be associated with this
     *      TreeTableCell.
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins. It is not common
     *       for developers or designers to access this function directly.
     */
    public final void updateTreeItem(TreeItem<T> treeItem) {
        TreeItem<T> _treeItem = getTreeItem();
        if (_treeItem != null) {
            _treeItem.leafProperty().removeListener(weakLeafListener);
        }
        setTreeItem(treeItem);
        if (treeItem != null) {
            treeItem.leafProperty().addListener(weakLeafListener);
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-table-row-cell";

    private static final PseudoClass EXPANDED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass COLLAPSED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("collapsed");

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeTableRowSkin<>(this);
    }


    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        final TreeItem<T> treeItem = getTreeItem();
        final TreeTableView<T> treeTableView = getTreeTableView();

        switch (attribute) {
            case TREE_ITEM_PARENT: {
                if (treeItem == null) return null;
                TreeItem<T> parent = treeItem.getParent();
                if (parent == null) return null;
                int parentIndex = treeTableView.getRow(parent);
                return treeTableView.queryAccessibleAttribute(AccessibleAttribute.ROW_AT_INDEX, parentIndex);
            }
            case TREE_ITEM_COUNT: {
                if (treeItem == null) return 0;
                if (!treeItem.isExpanded()) return 0;
                return treeItem.getChildren().size();
            }
            case TREE_ITEM_AT_INDEX: {
                if (treeItem == null) return null;
                if (!treeItem.isExpanded()) return null;
                int index = (Integer)parameters[0];
                if (index >= treeItem.getChildren().size()) return null;
                TreeItem<T> child = treeItem.getChildren().get(index);
                if (child == null) return null;
                int childIndex = treeTableView.getRow(child);
                return treeTableView.queryAccessibleAttribute(AccessibleAttribute.ROW_AT_INDEX, childIndex);
            }
            case LEAF: return treeItem == null ? true : treeItem.isLeaf();
            case EXPANDED: return treeItem == null ? false : treeItem.isExpanded();
            case INDEX: return getIndex();
            case DISCLOSURE_LEVEL: {
                return treeTableView == null ? 0 : treeTableView.getTreeItemLevel(treeItem);
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case EXPAND: {
                TreeItem<T> treeItem = getTreeItem();
                if (treeItem != null) treeItem.setExpanded(true);
                break;
            }
            case COLLAPSE: {
                TreeItem<T> treeItem = getTreeItem();
                if (treeItem != null) treeItem.setExpanded(false);
                break;
            }
            default: super.executeAccessibleAction(action);
        }
    }
}

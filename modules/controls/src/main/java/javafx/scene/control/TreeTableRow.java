/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.skin.VirtualContainerBase;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import com.sun.javafx.scene.control.skin.TreeTableRowSkin;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.accessibility.Action;
import javafx.scene.accessibility.Attribute;
import javafx.scene.accessibility.Role;
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
    
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TreeTableRow instance.
     */
    public TreeTableRow() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Callbacks and events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private final ListChangeListener<Integer> selectedListener = new ListChangeListener<Integer>() {
        @Override public void onChanged(ListChangeListener.Change<? extends Integer> c) {
            updateSelection();
        }
    };

    private final InvalidationListener focusedListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateFocus();
        }
    };

    private final InvalidationListener editingListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateEditing();
        }
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
    
    private final InvalidationListener treeItemExpandedInvalidationListener = new InvalidationListener() {
        @Override public void invalidated(Observable o) {
            final boolean expanded = ((BooleanProperty)o).get();
            pseudoClassStateChanged(EXPANDED_PSEUDOCLASS_STATE,   expanded);
            pseudoClassStateChanged(COLLAPSED_PSEUDOCLASS_STATE, !expanded);
        }
    };
    
    private final WeakListChangeListener<Integer> weakSelectedListener = 
            new WeakListChangeListener<Integer>(selectedListener);
    private final WeakInvalidationListener weakFocusedListener = 
            new WeakInvalidationListener(focusedListener);
    private final WeakInvalidationListener weakEditingListener = 
            new WeakInvalidationListener(editingListener);
    private final WeakInvalidationListener weakLeafListener = 
            new WeakInvalidationListener(leafListener);
    private final WeakInvalidationListener weakTreeItemExpandedInvalidationListener = 
            new WeakInvalidationListener(treeItemExpandedInvalidationListener);
    
    
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    // --- TreeItem
    private ReadOnlyObjectWrapper<TreeItem<T>> treeItem = 
        new ReadOnlyObjectWrapper<TreeItem<T>>(this, "treeItem") {
            
            TreeItem<T> oldValue = null;
            
            @Override protected void invalidated() {
                if (oldValue != null) {
                    oldValue.expandedProperty().removeListener(weakTreeItemExpandedInvalidationListener);
                }
                
                oldValue = get(); 
                
                if (oldValue != null) {
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
     * Returns the TreeItem currently set in this TreeCell.
     */
    public final TreeItem<T> getTreeItem() { return treeItem.get(); }
    
    /**
     * Each TreeTableCell represents at most a single {@link TreeItem}, which is
     * represented by this property.
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> treeItemProperty() { return treeItem.getReadOnlyProperty(); }

    
    
    // --- Disclosure Node
    private ObjectProperty<Node> disclosureNode = new SimpleObjectProperty<Node>(this, "disclosureNode");

    /**
     * The node to use as the "disclosure" triangle, or toggle, used for
     * expanding and collapsing items. This is only used in the case of
     * an item in the tree which contains child items. If not specified, the
     * TreeTableCell's Skin implementation is responsible for providing a default
     * disclosure node.
     */
    public final void setDisclosureNode(Node value) { disclosureNodeProperty().set(value); }
    
    /**
     * Returns the current disclosure node set in this TreeTableCell.
     */
    public final Node getDisclosureNode() { return disclosureNode.get(); }
    
    /**
     * The disclosure node is commonly seen represented as a triangle that rotates
     * on screen to indicate whether or not the TreeItem that it is placed
     * beside is expanded or collapsed.
     */
    public final ObjectProperty<Node> disclosureNodeProperty() { return disclosureNode; }
    
    
    // --- TreeView
    private ReadOnlyObjectWrapper<TreeTableView<T>> treeTableView = new ReadOnlyObjectWrapper<TreeTableView<T>>(this, "treeTableView") {
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
                
                weakTreeTableViewRef = new WeakReference<TreeTableView<T>>(get());
            }

            updateItem();
            requestLayout();
        }
    };
    
    private void setTreeTableView(TreeTableView<T> value) { treeTableView.set(value); }

    /**
     * Returns the TreeTableView associated with this TreeTableCell.
     */
    public final TreeTableView<T> getTreeTableView() { return treeTableView.get(); }
    
    /**
     * A TreeTableCell is explicitly linked to a single {@link TreeTableView} instance,
     * which is represented by this property.
     */
    public final ReadOnlyObjectProperty<TreeTableView<T>> treeTableViewProperty() { return treeTableView.getReadOnlyProperty(); }




    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    
    @Override void indexChanged() {
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
            treeTable.fireEvent(new TreeTableView.EditEvent<T>(treeTable,
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
            treeTable.fireEvent(new TreeTableView.EditEvent<T>(treeTable,
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
            treeTable.fireEvent(new TreeTableView.EditEvent<T>(treeTable,
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



    /***************************************************************************
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
        if (getTreeTableView().getSelectionModel() == null) return;
        
        boolean isSelected = getTreeTableView().getSelectionModel().isSelected(index);
        if (isSelected() == isSelected) return;
        
        updateSelected(isSelected);
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



    /***************************************************************************
     *                                                                         *
     * Expert API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Updates the TreeTableView associated with this TreeTableCell.
     * 
     * @param treeTable The new TreeTableView that should be associated with this
     *         TreeTableCell.
     * @expert This function is intended to be used by experts, primarily
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
     * @expert This function is intended to be used by experts, primarily
     *      by those implementing new Skins. It is not common
     *      for developers or designers to access this function directly.
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


    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-table-row-cell";

    private static final PseudoClass EXPANDED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass COLLAPSED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("collapsed");
    
    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeTableRowSkin<T>(this);
    }


    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** @treatAsPrivate */
    @Override public Object accGetAttribute(Attribute attribute, Object... parameters) {
        TreeItem<T> treeItem = getTreeItem();
        TreeTableView<T> treeTableView = getTreeTableView();

        switch (attribute) {
            case ROLE: return Role.TREE_TABLE_ITEM;
            case TREE_ITEM_PARENT: {
                if (treeItem == null) return null;
                TreeItem parent = treeItem.getParent();
                return parent == null ? treeTableView : getTreeTableRow(getVirtualFlow(), parent);
            }
            case TREE_ITEM_COUNT: {
                // response is relative to this tree cell
                return treeItem == null        ? 0 :
                       treeItem.isLeaf()       ? 0 :
                       ! treeItem.isExpanded() ? 0 :
                       treeItem.getChildren().size();
            }
            case TREE_ITEM_AT_INDEX: {
                // index is relative to this tree cell
                final int offset = (Integer)parameters[0];
                final int p = offset + getIndex();
                return treeItem == null                  ? null :
                       p > treeItem.getChildren().size() ? null :
                       getVirtualFlow().getCell(p);
            }
            case TITLE: {
                Object value = treeItem == null ? null : treeItem.getValue();
                return value == null ? "" : value.toString();
            }
            case LEAF: return treeItem == null ? true : treeItem.isLeaf();
            case EXPANDED: return treeItem == null ? false : treeItem.isExpanded();
            case INDEX: return getIndex();
            case SELECTED: return isSelected();
            case DISCLOSURE_LEVEL: {
                // FIXME replace with treeTableView.getTreeItemLevel(treeItem) when we sync up with 8u20
                return treeTableView == null ? 0 : TreeTableView.getNodeLevel(treeItem);
            }
            default: return super.accGetAttribute(attribute, parameters);
        }
    }

    /** @treatAsPrivate */
    @Override public void accExecuteAction(Action action, Object... parameters) {
        final TreeTableView<T> treeTableView = getTreeTableView();
        final TreeTableView.TreeTableViewSelectionModel<T> sm = treeTableView == null ? null : treeTableView.getSelectionModel();

        switch (action) {
            case SELECT: {
                if (sm != null) sm.clearAndSelect(getIndex());
                break;
            }
            case ADD_TO_SELECTION: {
                if (sm != null) sm.select(getIndex());
                break;
            }
            case REMOVE_FROM_SELECTION: {
                if (sm != null) sm.clearSelection(getIndex());
                break;
            }
            default: super.accExecuteAction(action);
        }
    }

    // returns the TreeTableRow instances used to represent the children of the
    // given TreeItem
    private List<Node> getTreeItemChildren(TreeItem<T> treeItem) {
        List<Node> children = new ArrayList<>();
        final VirtualFlow<TreeTableRow<T>> flow = getVirtualFlow();
        for (TreeItem childItem : treeItem.getChildren()) {
            TreeTableRow<T> row = getTreeTableRow(flow, childItem);

            // We should never, ever get row == null. If we do then
            // something is very wrong.
            assert row != null;

            // VirtualFlow should never return duplicates for different
            // indices, but I did see this happening. I don't want to
            // slow down normal use cases, but during development this
            // should be tested for.
            assert ! children.contains(row);

            if (row != null) children.add(row);
        }
        return children;
    }

    private VirtualFlow getVirtualFlow() {
        // FIXME Ugly hack! Clean this up once everything is understood
        Parent p = getParent();
        while (p != null && ! (p instanceof VirtualFlow)) {
            p = p.getParent();
        }

        if (p == null) {
            return null;
        }

        return (VirtualFlow) p;
    }

    private TreeTableRow<T> getTreeTableRow(VirtualFlow<TreeTableRow<T>> flow, TreeItem treeItem) {
        // FIXME Ugly hack! Clean this up once everything is understood
        final int treeItemIndex = getTreeTableView().getRow(treeItem);
        TreeTableRow<T> cell = null;

        if (flow != null) {
            cell = flow.getVisibleCell(treeItemIndex);
        }

        return cell;
    }
}

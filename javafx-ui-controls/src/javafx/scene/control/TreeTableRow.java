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
package javafx.scene.control;

import java.util.Set;
import javafx.css.PseudoClass;
import com.sun.javafx.scene.control.skin.TreeTableRowSkin;
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
import javafx.scene.Node;

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
        indexProperty().addListener(indexListener);
    }



    /***************************************************************************
     *                                                                         *
     * Callbacks and events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private final InvalidationListener indexListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            // when the cell index changes, this may result in the cell
            // changing state to be selected and/or focused.
            updateItem();
            updateSelection();
            updateFocus();
        }
    };

    private final ListChangeListener selectedListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
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
    
    private final WeakListChangeListener weakSelectedListener = new WeakListChangeListener(selectedListener);
    private final WeakInvalidationListener weakFocusedListener = new WeakInvalidationListener(focusedListener);
    private final WeakInvalidationListener weakEditingListener = new WeakInvalidationListener(editingListener);
    
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    // --- TreeItem
    private ReadOnlyObjectWrapper<TreeItem<T>> treeItem = 
        new ReadOnlyObjectWrapper<TreeItem<T>>(this, "treeItem") {
            
            TreeItem<T> oldValue = null;
            
            @Override public void setValue(TreeItem<T> value) {
                
                if (oldValue != null) {
                    oldValue.expandedProperty().removeListener(treeItemExpandedInvalidationListener);
                }
                
                treeItem.set(value); 
                
                if (value != null) {
                    value.expandedProperty().addListener(treeItemExpandedInvalidationListener);
                    // fake an invalidation to ensure updated pseudo-class state
                    treeItemExpandedInvalidationListener.invalidated(value.expandedProperty());            
                }
                
                oldValue = value;
                
            }
            
    };
    private void setTreeItem(TreeItem<T> value) {
        treeItem.set(value); 
    }
    
    private InvalidationListener treeItemExpandedInvalidationListener = 
            new InvalidationListener() {

        @Override
        public void invalidated(Observable o) {
            final boolean expanded = ((BooleanProperty)o).get();
            pseudoClassStateChanged(EXPANDED_PSEUDOCLASS_STATE,   expanded);
            pseudoClassStateChanged(COLLAPSED_PSEUDOCLASS_STATE, !expanded);
        }
                
    };
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
            MultipleSelectionModel sm;
            FocusModel fm;
            
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

                    oldTreeTableView.editingItemProperty().removeListener(weakEditingListener);
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

                get().editingItemProperty().addListener(weakEditingListener);
                
                weakTreeTableViewRef = new WeakReference<TreeTableView<T>>(get());
            }

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
        final TreeItem treeItem = getTreeItem();
        final TreeTableView treeTable = getTreeTableView();
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
            treeTable.edit(null);
            treeTable.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;
        
        TreeTableView treeTable = getTreeTableView();
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
            treeTable.edit(null);
            treeTable.requestFocus();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/
    
    private void updateItem() {
        TreeTableView<T> tv = getTreeTableView();
        if (tv == null) return;
        
        // Compute whether the index for this cell is for a real item
        boolean valid = getIndex() >=0 && getIndex() < tv.getExpandedItemCount();

        // get the new treeItem that is about to go in to the TreeCell
        TreeItem<T> treeItem = valid ? tv.getTreeItem(getIndex()) : null;
        
        // Cause the cell to update itself
        if (valid && treeItem != null) {
            // update the TreeCell state.
            // For the sake of RT-14279, it is important that the order of these
            // method calls is as shown below. If the order is switched, it is
            // likely that events will be fired where the item is null, even
            // though calling cell.getTreeItem().getValue() returns the value
            // as expected
            updateTreeItem(treeItem);
            updateItem(treeItem.getValue(), false);
        } else {
            updateTreeItem(null);
            updateItem(null, true);
        }
    }

    private void updateSelection() {
        if (getIndex() == -1 || getTreeTableView() == null) return;
        if (getTreeTableView().getSelectionModel() == null) return;
        
        updateSelected(getTreeTableView().getSelectionModel().isSelected(getIndex()));
    }

    private void updateFocus() {
        if (getIndex() == -1 || getTreeTableView() == null) return;
        if (getTreeTableView().getFocusModel() == null) return;
        
        setFocused(getTreeTableView().getFocusModel().isFocused(getIndex()));
    }

    private void updateEditing() {
        if (getIndex() == -1 || getTreeTableView() == null || getTreeItem() == null) return;
        
        TreeItem editItem = getTreeTableView().getEditingItem();
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
     * @param tree The new TreeTableView that should be associated with this 
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
        setTreeItem(treeItem);
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
}

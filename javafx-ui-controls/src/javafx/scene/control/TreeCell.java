/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import com.sun.javafx.scene.control.skin.TreeCellSkin;
import javafx.collections.WeakListChangeListener;
import java.lang.ref.WeakReference;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;

/**
 * The {@link Cell} type used with the {@link TreeView} control. In addition to 
 * the API defined on {@link IndexedCell}, the TreeCell
 * exposes additional states and pseudo classes for use by CSS.
 * <p>
 * A TreeCell watches the selection model of the TreeView for which it is
 * associated, ensuring that it visually indicates to the user whether it is
 * selected. When a TreeCell is selected, this is exposed both via the
 * {@link #selectedProperty() selected} property, as well as via the 'selected'
 * CSS pseudo class state.
 * <p>
 * Due to the fact that TreeCell extends from {@link IndexedCell}, each TreeCell 
 * also provides an {@link #indexProperty() index} property. The index will be 
 * updated as cells are expanded and collapsed, and therefore should be
 * considered a view index rather than a model index.
 * <p>
 * Finally, each TreeCell also has a reference back to the TreeView that it is
 * being used with. Each TreeCell belongs to one and only one TreeView.
 *
 * @see TreeView
 * @see TreeItem
 * @param <T> The type of the value contained within the 
 *      {@link #treeItemProperty() TreeItem} property.
 */
public class TreeCell<T> extends IndexedCell<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TreeCell instance.
     */
    public TreeCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Callbacks and events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private final ListChangeListener selectedListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
            updateSelection();
        }
    };
    
    /**
     * Listens to the selectionModel property on the TreeView. Whenever the entire model is changed,
     * we have to unhook the weakSelectedListener and update the selection.
     */
    private final ChangeListener selectionModelPropertyListener = new ChangeListener<MultipleSelectionModel>() {
        @Override public void changed(ObservableValue observable,
                                      MultipleSelectionModel oldValue,
                                      MultipleSelectionModel newValue) {
            if (oldValue != null) {
                oldValue.getSelectedIndices().removeListener(weakSelectedListener);
            }
            if (newValue != null) {
                newValue.getSelectedIndices().addListener(weakSelectedListener);
            }
            updateSelection();
        }
    };    

    private final InvalidationListener focusedListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateFocus();
        }
    };
    
    /**
     * Listens to the focusModel property on the TreeView. Whenever the entire model is changed,
     * we have to unhook the weakFocusedListener and update the focus.
     */
    private final ChangeListener focusModelPropertyListener = new ChangeListener<FocusModel>() {
        @Override public void changed(ObservableValue observable,
                                      FocusModel oldValue,
                                      FocusModel newValue) {
            if (oldValue != null) {
                oldValue.focusedIndexProperty().removeListener(weakFocusedListener);
            }
            if (newValue != null) {
                newValue.focusedIndexProperty().addListener(weakFocusedListener);
            }
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
            TreeItem treeItem = getTreeItem();
            if (treeItem != null) {
                requestLayout();
            }
        }
    };
    
    private final WeakListChangeListener weakSelectedListener = new WeakListChangeListener(selectedListener);
    private final WeakChangeListener weakSelectionModelPropertyListener = new WeakChangeListener(selectionModelPropertyListener);
    private final WeakInvalidationListener weakFocusedListener = new WeakInvalidationListener(focusedListener);
    private final WeakChangeListener weakFocusModelPropertyListener = new WeakChangeListener(focusModelPropertyListener);
    private final WeakInvalidationListener weakEditingListener = new WeakInvalidationListener(editingListener);
    private final WeakInvalidationListener weakLeafListener = new WeakInvalidationListener(leafListener);
    
    
    
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
    
    /**
     * Returns the TreeItem currently set in this TreeCell.
     */
    public final TreeItem<T> getTreeItem() { return treeItem.get(); }
    
    /**
     * Each TreeCell represents at most a single {@link TreeItem}, which is
     * represented by this property.
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> treeItemProperty() { return treeItem.getReadOnlyProperty(); }


    /* proxy pseudo-class state change from treeItem's expandedProperty */
    private final InvalidationListener treeItemExpandedInvalidationListener = 
        new InvalidationListener() {

        @Override
        public void invalidated(Observable o) {
            boolean isExpanded = ((BooleanProperty)o).get();
            pseudoClassStateChanged(EXPANDED_PSEUDOCLASS_STATE,   isExpanded);
            pseudoClassStateChanged(COLLAPSED_PSEUDOCLASS_STATE, !isExpanded);
        }
    };
    
    
    
    // --- Disclosure Node
    private ObjectProperty<Node> disclosureNode = new SimpleObjectProperty<Node>(this, "disclosureNode");

    /**
     * The node to use as the "disclosure" triangle, or toggle, used for
     * expanding and collapsing items. This is only used in the case of
     * an item in the tree which contains child items. If not specified, the
     * TreeCell's Skin implementation is responsible for providing a default
     * disclosure node.
     */
    public final void setDisclosureNode(Node value) { disclosureNodeProperty().set(value); }
    
    /**
     * Returns the current disclosure node set in this TreeCell.
     */
    public final Node getDisclosureNode() { return disclosureNode.get(); }
    
    /**
     * The disclosure node is commonly seen represented as a triangle that rotates
     * on screen to indicate whether or not the TreeItem that it is placed
     * beside is expanded or collapsed.
     */
    public final ObjectProperty<Node> disclosureNodeProperty() { return disclosureNode; }
    
    
    // --- TreeView
    private ReadOnlyObjectWrapper<TreeView<T>> treeView = new ReadOnlyObjectWrapper<TreeView<T>>() {
        private WeakReference<TreeView<T>> weakTreeViewRef;
        @Override protected void invalidated() {
            MultipleSelectionModel sm;
            FocusModel fm;
            
            if (weakTreeViewRef != null) {
                TreeView<T> oldTreeView = weakTreeViewRef.get();
                if (oldTreeView != null) {
                    // remove old listeners
                    sm = oldTreeView.getSelectionModel();
                    if (sm != null) {
                        sm.getSelectedIndices().removeListener(weakSelectedListener);
                    }

                    fm = oldTreeView.getFocusModel();
                    if (fm != null) {
                        fm.focusedIndexProperty().removeListener(weakFocusedListener);
                    }

                    oldTreeView.editingItemProperty().removeListener(weakEditingListener);
                    oldTreeView.focusModelProperty().removeListener(weakFocusModelPropertyListener);
                    oldTreeView.selectionModelProperty().removeListener(weakSelectionModelPropertyListener);
                }
                
                weakTreeViewRef = null;
            }

            TreeView treeView = get();
            if (treeView != null) {
                sm = treeView.getSelectionModel();
                if (sm != null) {
                    // listening for changes to treeView.selectedIndex and IndexedCell.index,
                    // to determine if this cell is selected
                    sm.getSelectedIndices().addListener(weakSelectedListener);
                }

                fm = treeView.getFocusModel();
                if (fm != null) {
                    // similar to above, but this time for focus
                    fm.focusedIndexProperty().addListener(weakFocusedListener);
                }

                treeView.editingItemProperty().addListener(weakEditingListener);
                treeView.focusModelProperty().addListener(weakFocusModelPropertyListener);
                treeView.selectionModelProperty().addListener(weakSelectionModelPropertyListener);
                
                weakTreeViewRef = new WeakReference<TreeView<T>>(treeView);
            }

            updateItem();
            requestLayout();
        }

        @Override
        public Object getBean() {
            return TreeCell.this;
        }

        @Override
        public String getName() {
            return "treeView";
        }
    };
    
    private void setTreeView(TreeView<T> value) { treeView.set(value); }

    /**
     * Returns the TreeView associated with this TreeCell.
     */
    public final TreeView<T> getTreeView() { return treeView.get(); }
    
    /**
     * A TreeCell is explicitly linked to a single {@link TreeView} instance,
     * which is represented by this property.
     */
    public final ReadOnlyObjectProperty<TreeView<T>> treeViewProperty() { return treeView.getReadOnlyProperty(); }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        final TreeView<T> tree = getTreeView();
        if (! isEditable() || (tree != null && ! tree.isEditable())) {
//            if (Logging.getControlsLogger().isLoggable(PlatformLogger.SEVERE)) {
//                Logging.getControlsLogger().severe(
//                    "Can not call TreeCell.startEdit() on this TreeCell, as it "
//                        + "is not allowed to enter its editing state (TreeCell: "
//                        + this + ", TreeView: " + tree + ").");
//            }
            return;
        }
        
        // it makes sense to get the cell into its editing state before firing
        // the event to the TreeView below, so that's what we're doing here
        // by calling super.startEdit().
        super.startEdit();
        
         // Inform the TreeView of the edit starting.
        if (tree != null) {
            tree.fireEvent(new TreeView.EditEvent<T>(tree,
                    TreeView.<T>editStartEvent(),
                    getTreeItem(),
                    getItem(),
                    null));
            
            tree.requestFocus();
        }
    }

     /** {@inheritDoc} */
    @Override public void commitEdit(T newValue) {
        if (! isEditing()) return;
        final TreeItem treeItem = getTreeItem();
        final TreeView tree = getTreeView();
        if (tree != null) {
            // Inform the TreeView of the edit being ready to be committed.
            tree.fireEvent(new TreeView.EditEvent<T>(tree,
                    TreeView.<T>editCommitEvent(),
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

        if (tree != null) {
            // reset the editing item in the TreetView
            tree.edit(null);
            tree.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;
        
        TreeView tree = getTreeView();

        super.cancelEdit();

        if (tree != null) {
            // reset the editing index on the TreeView
            if (updateEditingIndex) tree.edit(null);
            tree.requestFocus();
        
            tree.fireEvent(new TreeView.EditEvent<T>(tree,
                    TreeView.<T>editCancelEvent(),
                    getTreeItem(),
                    getItem(),
                    null));
        }
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeCellSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/
    
    /** {@inheritDoc} */
    @Override void indexChanged() {
        super.indexChanged();
        
        // when the cell index changes, this may result in the cell
        // changing state to be selected and/or focused.
        updateItem();
        updateSelection();
        updateFocus();
    }
    
    private void updateItem() {
        TreeView<T> tv = getTreeView();
        if (tv == null) return;
        
        // Compute whether the index for this cell is for a real item
        boolean valid = getIndex() >=0 && getIndex() < tv.getExpandedItemCount();

        // Cause the cell to update itself
        if (valid) {
            // update the TreeCell state.
            // get the new treeItem that is about to go in to the TreeCell
            TreeItem<T> _treeItem = tv.getTreeItem(getIndex());
        
            // For the sake of RT-14279, it is important that the order of these
            // method calls is as shown below. If the order is switched, it is
            // likely that events will be fired where the item is null, even
            // though calling cell.getTreeItem().getValue() returns the value
            // as expected
            if (_treeItem == null || ! _treeItem.equals(getTreeItem())) {
                updateTreeItem(_treeItem);
                updateItem(_treeItem == null ? null : _treeItem.getValue(), false);
            }
        } else {
            updateTreeItem(null);
            updateItem(null, true);
        }
    }

    private void updateSelection() {
        if (isEmpty()) return;
        if (getIndex() == -1 || getTreeView() == null) return;
        if (getTreeView().getSelectionModel() == null) return;
        
        boolean isSelected = getTreeView().getSelectionModel().isSelected(getIndex());
        if (isSelected() == isSelected) return;
        
        updateSelected(isSelected);
    }

    private void updateFocus() {
        if (getIndex() == -1 || getTreeView() == null) return;
        if (getTreeView().getFocusModel() == null) return;
        
        setFocused(getTreeView().getFocusModel().isFocused(getIndex()));
    }

    private boolean updateEditingIndex = true;
    private void updateEditing() {
        final int index = getIndex();
        final TreeView tree = getTreeView();
        final TreeItem treeItem = getTreeItem();
        final TreeItem editItem = tree == null ? null : tree.getEditingItem();
        final boolean editing = isEditing();
        
        if (index == -1 || tree == null || treeItem == null) return;
        
        final boolean match = treeItem.equals(editItem);
        
        // If my tree item is the item being edited and I'm not currently in
        // the edit mode, then I need to enter the edit mode
        if (match && !editing) {
            startEdit();
        } else if (match && editing) {
            // If my tree item is not the one being edited then I need to cancel
            // the edit. The tricky thing here is that as part of this call
            // I cannot end up calling tree.edit(null) the way that the standard
            // cancelEdit method would do. Yet, I need to call cancelEdit
            // so that subclasses which override cancelEdit can execute. So,
            // I have to use a kind of hacky flag workaround.
            updateEditingIndex = false;
            cancelEdit();
            updateEditingIndex = true;
        }
    }



    /***************************************************************************
     *                                                                         *
     * Expert API                                                              *
     *                                                                         *
     **************************************************************************/
    
    

    /**
     * Updates the TreeView associated with this TreeCell.
     * 
     * @param tree The new TreeView that should be associated with this TreeCell.
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTreeView(TreeView<T> tree) {
        setTreeView(tree); 
    }

    /**
     * Updates the TreeItem associated with this TreeCell.
     *
     * @param treeItem The new TreeItem that should be associated with this 
     *      TreeCell.
     * @expert This function is intended to be used by experts, primarily
     *      by those implementing new Skins. It is not common
     *      for developers or designers to access this function directly.
     */
    public final void updateTreeItem(TreeItem<T> treeItem) {
        TreeItem _treeItem = getTreeItem();
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

    private static final String DEFAULT_STYLE_CLASS = "tree-cell";

    private static final PseudoClass EXPANDED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass COLLAPSED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("collapsed");

}

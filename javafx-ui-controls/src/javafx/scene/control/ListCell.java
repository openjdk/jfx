/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.sun.javafx.scene.control.WeakListChangeListener;
import java.lang.ref.WeakReference;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;


/**
 * <p>The {@link Cell} type used within {@link ListView} instances. In addition 
 * to the API defined on Cell and {@link IndexedCell}, the ListCell is more 
 * tightly bound to a ListView, allowing for better support of editing events, 
 * etc.
 *
 * <p>A ListView maintains selection, indicating which cell(s) have been selected,
 * and focus, indicating the current focus owner for any given ListView. For each
 * property, each ListCell has a boolean reflecting whether this specific cell is
 * selected or focused. To achieve this, each ListCell has a reference back to
 * the ListView that it is being used within. Each ListCell belongs to one and 
 * only one ListView.
 * 
 * <p>Note that in the case of virtualized controls like ListView, when a cell
 * has focus this is not in the same sense as application focus. When a ListCell 
 * has focus it simply represents the fact that the cell will  receive keyboard
 * events in the situation that the owning ListView actually contains focus. Of
 * course, in the case where a cell has a Node set in the 
 * {@link #graphicProperty() graphic} property, it is completely legal for this
 * Node to request, and acquire focus as would normally be expected.
 * 
 * @param <T> The type of the item contained within the ListCell.
 */
// TODO add code examples
public class ListCell<T> extends IndexedCell<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ListCell with the default style class of 'list-cell'.
     */
    public ListCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        indexProperty().addListener(indexListener);
    }


    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *     We have to listen to a number of properties on the ListView itself  *
     *     as well as attach listeners to a couple different ObservableLists.  *
     *     We have to be sure to unhook these listeners whenever the reference *
     *     to the ListView changes, or whenever one of the ObservableList      *
     *     references changes (such as setting the selectionModel, focusModel, *
     *     or items).                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Listens to the index changing (on the super class). Whenever the index has
     * been changed, we need to update the item, and potentially the selection and
     * focus as well.
     */
    private InvalidationListener indexListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            indexChanged();
         }
     };
    
    @Override void indexChanged() {
        updateItem();
        updateSelection();
        updateFocus();
    }

    /**
     * Listens to the editing index on the ListView. It is possible for the developer
     * to call the ListView#edit(int) method and cause a specific cell to start
     * editing. In such a case, we need to be notified so we can call startEdit
     * on our side.
     */
    private final InvalidationListener editingListener = new InvalidationListener() {
        @Override public void invalidated(Observable value) {
            final int index = getIndex();
            final ListView list = getListView();
            final int editIndex = list == null ? -1 : list.getEditingIndex();
            final boolean editing = isEditing();

            // Check that the list is specified, and my index is not -1
            if (index != -1 && list != null) {
                // If my index is the index being edited and I'm not currently in
                // the edit mode, then I need to enter the edit mode
                if (index == editIndex && !editing) {
                    startEdit();
                } else if (index != editIndex && editing) {
                    // If my index is not the one being edited then I need to cancel
                    // the edit. The tricky thing here is that as part of this call
                    // I cannot end up calling list.edit(-1) the way that the standard
                    // cancelEdit method would do. Yet, I need to call cancelEdit
                    // so that subclasses which override cancelEdit can execute. So,
                    // I have to use a kind of hacky flag workaround.
                    updateEditingIndex = false;
                    cancelEdit();
                    updateEditingIndex = true;
                }
            }
        }
    };
    private boolean updateEditingIndex = true;

    /**
     * Listens to the selection model on the ListView. Whenever the selection model
     * is changed (updated), the selected property on the ListCell is updated accordingly.
     */
    private final ListChangeListener selectedListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            updateSelection();
        }
    };

    /**
     * Listens to the selectionModel property on the ListView. Whenever the entire model is changed,
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

    /**
     * Listens to the items on the ListView. Whenever the items are changed in such a way that
     * it impacts the index of this ListCell, then we must update the item.
     */
    private final ListChangeListener itemsListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            updateItem(); // TODO limit this to only those changes necessary
        }
    };

    /**
     * Listens to the items property on the ListView. Whenever the entire list is changed,
     * we have to unhook the weakItemsListener and update the item.
     */
    private final ChangeListener itemsPropertyListener = new ChangeListener<ObservableList>() {
        @Override public void changed(ObservableValue observable,
                                      ObservableList oldValue,
                                      ObservableList newValue) {
            if (oldValue != null) {
                oldValue.removeListener(weakItemsListener);
            }
            if (newValue != null) {
                newValue.addListener(weakItemsListener);
            }
            updateItem();
        }
    };

    /**
     * Listens to the focus model on the ListView. Whenever the focus model changes,
     * the focused property on the ListCell is updated
     */
    private final InvalidationListener focusedListener = new InvalidationListener() {
        @Override public void invalidated(Observable value) {
            updateFocus();
        }
    };

    /**
     * Listens to the focusModel property on the ListView. Whenever the entire model is changed,
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


    private final WeakInvalidationListener weakEditingListener = new WeakInvalidationListener(editingListener);
    private final WeakListChangeListener weakSelectedListener = new WeakListChangeListener(selectedListener);
    private final WeakChangeListener weakSelectionModelPropertyListener = new WeakChangeListener(selectionModelPropertyListener);
    private final WeakListChangeListener weakItemsListener = new WeakListChangeListener(itemsListener);
    private final WeakChangeListener weakItemsPropertyListener = new WeakChangeListener(itemsPropertyListener);
    private final WeakInvalidationListener weakFocusedListener = new WeakInvalidationListener(focusedListener);
    private final WeakChangeListener weakFocusModelPropertyListener = new WeakChangeListener(focusModelPropertyListener);

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * The ListView associated with this Cell.
     */
    private ReadOnlyObjectWrapper<ListView<T>> listView = new ReadOnlyObjectWrapper<ListView<T>>(this, "listView") {
        /**
         * A weak reference to the ListView itself, such that whenever the ...
         */
        private WeakReference<ListView<T>> weakListViewRef = new WeakReference<ListView<T>>(null);

        @Override protected void invalidated() {
            // Get the current and old list view references
            final ListView<T> currentListView = get();
            final ListView<T> oldListView = weakListViewRef.get();

            // If the currentListView is the same as the oldListView, then
            // there is nothing to be done.
            if (currentListView == oldListView) return;

            // If the old list view is not null, then we must unhook all its listeners
            if (oldListView != null) {
                // If the old selection model isn't null, unhook it
                final MultipleSelectionModel sm = oldListView.getSelectionModel();
                if (sm != null) {
                    sm.getSelectedIndices().removeListener(weakSelectedListener);
                }

                // If the old focus model isn't null, unhook it
                final FocusModel fm = oldListView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().removeListener(weakFocusedListener);
                }

                // If the old items isn't null, unhook the listener
                final ObservableList items = oldListView.getItems();
                if (items != null) {
                    items.removeListener(weakItemsListener);
                }

                // Remove the listeners of the properties on ListView
                oldListView.editingIndexProperty().removeListener(weakEditingListener);
                oldListView.itemsProperty().removeListener(weakItemsPropertyListener);
                oldListView.focusModelProperty().removeListener(weakFocusModelPropertyListener);
                oldListView.selectionModelProperty().removeListener(weakSelectionModelPropertyListener);
            }

            if (currentListView != null) {
                final MultipleSelectionModel sm = currentListView.getSelectionModel();
                if (sm != null) {
                    sm.getSelectedIndices().addListener(weakSelectedListener);
                }

                final FocusModel fm = currentListView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().addListener(weakFocusedListener);
                }

                final ObservableList items = currentListView.getItems();
                if (items != null) {
                    items.addListener(weakItemsListener);
                }

                currentListView.editingIndexProperty().addListener(weakEditingListener);
                currentListView.itemsProperty().addListener(weakItemsPropertyListener);
                currentListView.focusModelProperty().addListener(weakFocusModelPropertyListener);
                currentListView.selectionModelProperty().addListener(weakSelectionModelPropertyListener);

                weakListViewRef = new WeakReference<ListView<T>>(currentListView);
            }

            updateItem();
            updateSelection();
            updateFocus();
            requestLayout();
        }
    };
    private void setListView(ListView<T> value) { listView.set(value); }
    public final ListView<T> getListView() { return listView.get(); }
    public final ReadOnlyObjectProperty<ListView<T>> listViewProperty() { return listView.getReadOnlyProperty(); }


    /***************************************************************************
     *                                                                         *
     * Editing API                                                             *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        final ListView list = getListView();
        if (!isEditable() || (list != null && ! list.isEditable())) {
            return;
        }
        
        // it makes sense to get the cell into its editing state before firing
        // the event to the ListView below, so that's what we're doing here
        // by calling super.startEdit().
        super.startEdit();
        
         // Inform the ListView of the edit starting.
        if (list != null) {
            list.fireEvent(new ListView.EditEvent<T>(list,
                    ListView.<T>editStartEvent(),
                    null,
                    list.getEditingIndex()));
            list.edit(getIndex());
            list.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void commitEdit(T newValue) {
        if (! isEditing()) return;
        ListView list = getListView();
        
        if (list != null) {
            // Inform the ListView of the edit being ready to be committed.
            list.fireEvent(new ListView.EditEvent<T>(list,
                    ListView.<T>editCommitEvent(),
                    newValue,
                    list.getEditingIndex()));
        }
        
        // update the item within this cell, so that it represents the new value
        updateItem(newValue, false);

        // inform parent classes of the commit, so that they can switch us
        // out of the editing state
        super.commitEdit(newValue);
        
        if (list != null) {
            // reset the editing index on the ListView. This must come after the
            // event is fired so that the developer on the other side can consult
            // the ListView editingIndex property (if they choose to do that
            // rather than just grab the int from the event).
            list.edit(-1);
            list.requestFocus();
        }
    }
    
    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;
        
         // Inform the ListView of the edit being cancelled.
        ListView list = getListView();
        
        super.cancelEdit();

        if (list != null) {
            int editingIndex = list.getEditingIndex();
            
            // reset the editing index on the ListView
            if (updateEditingIndex) list.edit(-1);
            list.requestFocus();
        
            list.fireEvent(new ListView.EditEvent<T>(list,
                    ListView.<T>editCancelEvent(),
                    null,
                    editingIndex));
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/
   
    private void updateItem() {
        ListView<T> lv = getListView();
        List<T> items = lv == null ? null : lv.getItems();

        // Compute whether the index for this cell is for a real item
        boolean valid = items != null && getIndex() >=0 && getIndex() < items.size();

        // Cause the cell to update itself
        if (valid) {
            T newItem = items.get(getIndex());
            if (newItem == null || ! newItem.equals(getItem())) {
                updateItem(newItem, false);
            }
        } else {
            updateItem(null, true);
        }
    }
    
    /**
     * Updates the ListView associated with this Cell.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateListView(ListView<T> listView) {
        setListView(listView);
    }

    private void updateSelection() {
        if (isEmpty()) return;
        if (getIndex() == -1 || getListView() == null) return;
        if (getListView().getSelectionModel() == null) return;
        
        boolean isSelected = getListView().getSelectionModel().isSelected(getIndex());
        if (isSelected() == isSelected) return;
        
        updateSelected(getListView().getSelectionModel().isSelected(getIndex()));
    }

    private void updateFocus() {
        if (getIndex() == -1 || getListView() == null) return;
        if (getListView().getFocusModel() == null) return;
        
        setFocused(getListView().getFocusModel().isFocused(getIndex()));
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "list-cell";
}


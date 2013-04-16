/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.skin.ListCellSkin;
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

import javafx.collections.WeakListChangeListener;
import java.lang.ref.WeakReference;
import java.util.List;


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

    private boolean updateEditingIndex = true;

    /**
     * Listens to the selectionModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakSelectedListener and update the selection.
     */
    private final ChangeListener<MultipleSelectionModel<T>> selectionModelPropertyListener = new ChangeListener<MultipleSelectionModel<T>>() {
        @Override
        public void changed(
                ObservableValue<? extends MultipleSelectionModel<T>> observable,
                MultipleSelectionModel<T> oldValue,
                MultipleSelectionModel<T> newValue) {
            
            if (oldValue != null) {
                getPropertyListener().unregisterChangeListener(oldValue.getSelectedIndices());
            }
            
            if (newValue != null) {
                getPropertyListener().registerChangeListener(newValue.getSelectedIndices(), "SELECTED_INDICES");
            }
            
            updateSelection();
        }
        
    };

    /**
     * Listens to the items property on the ListView. Whenever the entire list is changed,
     * we have to unhook the weakItemsListener and update the item.
     */
    private final ChangeListener<ObservableList<T>> itemsPropertyListener = new ChangeListener<ObservableList<T>>() {
        @Override public void changed(ObservableValue<? extends ObservableList<T>> observable,
                                      ObservableList<T> oldValue,
                                      ObservableList<T> newValue) {
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
     * Listens to the focusModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakFocusedListener and update the focus.
     */
    private final ChangeListener<FocusModel<T>> focusModelPropertyListener = new ChangeListener<FocusModel<T>>() {
        @Override public void changed(ObservableValue<? extends FocusModel<T>> observable,
                                      FocusModel<T> oldValue,
                                      FocusModel<T> newValue) {
            if (oldValue != null) {
                getPropertyListener().unregisterChangeListener(oldValue.focusedIndexProperty());
            }
            if (newValue != null) {
                getPropertyListener().registerChangeListener(newValue.focusedIndexProperty(), "FOCUSED_INDEX");
            }
            updateFocus();
        }
    };
    
    // This can not be extracted out into the property listener, as it is provided
    // by the user, and property listener needs to iterate over all values to 
    // determine if the list is already observed.
    private final ListChangeListener<T> itemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(ListChangeListener.Change<? extends T> c) {
            updateItem();
        }
    };

    private final WeakChangeListener<MultipleSelectionModel<T>> weakSelectionModelPropertyListener = new WeakChangeListener<MultipleSelectionModel<T>>(selectionModelPropertyListener);
    private final WeakChangeListener<ObservableList<T>> weakItemsPropertyListener = new WeakChangeListener<ObservableList<T>>(itemsPropertyListener);
    private final WeakChangeListener<FocusModel<T>> weakFocusModelPropertyListener = new WeakChangeListener<FocusModel<T>>(focusModelPropertyListener);
    private final WeakListChangeListener<T> weakItemsListener = new WeakListChangeListener<T>(itemsListener);
    
    
    
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
                final MultipleSelectionModel<T> sm = oldListView.getSelectionModel();
                if (sm != null) {
                    getPropertyListener().unregisterChangeListener(sm.getSelectedIndices());
                }

                // If the old focus model isn't null, unhook it
                final FocusModel<T> fm = oldListView.getFocusModel();
                if (fm != null) {
                    getPropertyListener().unregisterChangeListener(fm.focusedIndexProperty());
                }

                // If the old items isn't null, unhook the listener
                final ObservableList<T> items = oldListView.getItems();
                if (items != null) {
                    items.removeListener(weakItemsListener);
                }

                // Remove the listeners of the properties on ListView
                getPropertyListener().unregisterChangeListener(oldListView.editingIndexProperty());
                oldListView.itemsProperty().removeListener(weakItemsPropertyListener);
                oldListView.focusModelProperty().removeListener(weakFocusModelPropertyListener);
                oldListView.selectionModelProperty().removeListener(weakSelectionModelPropertyListener);
            }

            if (currentListView != null) {
                final MultipleSelectionModel<T> sm = currentListView.getSelectionModel();
                if (sm != null) {
                    getPropertyListener().registerChangeListener(sm.getSelectedIndices(), "SELECTED_INDICES");
                }

                final FocusModel<T> fm = currentListView.getFocusModel();
                if (fm != null) {
                    getPropertyListener().registerChangeListener(fm.focusedIndexProperty(), "FOCUSED_INDEX");
                }

                final ObservableList<T> items = currentListView.getItems();
                if (items != null) {
                    items.addListener(weakItemsListener);
                }

                getPropertyListener().registerChangeListener(currentListView.editingIndexProperty(), "EDITING_INDEX");
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
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /** {@inheritDoc} */
    @Override void indexChanged() {
        super.indexChanged();
        updateItem();
        updateSelection();
        updateFocus();
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ListCellSkin(this);
    }


    /***************************************************************************
     *                                                                         *
     * Editing API                                                             *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        final ListView<T> list = getListView();
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
        ListView<T> list = getListView();
        
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
        ListView<T> list = getListView();
        
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
        int index = getIndex();
        
        // Compute whether the index for this cell is for a real item
        boolean valid = items != null && index >=0 && index < items.size();

        // Cause the cell to update itself
        if (valid) {
            T oldValue = getItem();
            T newValue = items.get(index);
            
            if ((newValue != null && ! newValue.equals(oldValue)) || 
                    oldValue != null && ! oldValue.equals(newValue)) {
                updateItem(newValue, false);
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
        int index = getIndex();
        ListView<T> listView = getListView();
        if (index == -1 || listView == null) return;
        
        SelectionModel<T> sm = listView.getSelectionModel();
        if (sm == null) return;
        
        boolean isSelected = sm.isSelected(index);
        if (isSelected() == isSelected) return;
        
        updateSelected(isSelected);
    }

    private void updateFocus() {
        int index = getIndex();
        ListView<T> listView = getListView();
        if (index == -1 || listView == null) return;
        
        FocusModel<T> fm = listView.getFocusModel();
        if (fm == null) return;
        
        setFocused(fm.isFocused(index));
    }
    
    private void updateEditing() {
        final int index = getIndex();
        final ListView<T> list = getListView();
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
     
    
    @Override void handlePropertyChanged(final String p) {
        super.handlePropertyChanged(p);
        
        if ("SELECTED_INDICES".equals(p)) {
            updateSelection();
        } else if ("FOCUSED_INDEX".equals(p)) {
            updateFocus();
        } else if ("EDITING_INDEX".equals(p)) {
            updateEditing();
        }
    }

    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "list-cell";
}


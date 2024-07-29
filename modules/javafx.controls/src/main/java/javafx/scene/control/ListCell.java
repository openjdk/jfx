/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.List;

import javafx.application.Platform;
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
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;

import javafx.scene.control.skin.ListCellSkin;

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
 * @since JavaFX 2.0
 */
// TODO add code examples
public class ListCell<T> extends IndexedCell<T> {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ListCell with the default style class of 'list-cell'.
     */
    public ListCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.LIST_ITEM);
    }


    /* *************************************************************************
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
     * Listens to the editing index on the ListView. It is possible for the developer
     * to call the ListView#edit(int) method and cause a specific cell to start
     * editing. In such a case, we need to be notified so we can call startEdit
     * on our side.
     */
    private final InvalidationListener editingListener = value -> {
        updateEditing();
    };
    private boolean updateEditingIndex = true;

    /**
     * Listens to the selection model on the ListView. Whenever the selection model
     * is changed (updated), the selected property on the ListCell is updated accordingly.
     */
    private final ListChangeListener<Integer> selectedListener = c -> {
        updateSelection();
    };

    /**
     * Listens to the selectionModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakSelectedListener and update the selection.
     */
    private final ChangeListener<MultipleSelectionModel<T>> selectionModelPropertyListener = new ChangeListener<>() {
        @Override
        public void changed(
                ObservableValue<? extends MultipleSelectionModel<T>> observable,
                MultipleSelectionModel<T> oldValue,
                MultipleSelectionModel<T> newValue) {

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
    private final ListChangeListener<T> itemsListener = c -> {
        boolean doUpdate = false;
        while (c.next()) {
            // RT-35395: We only update the item in this cell if the current cell
            // index is within the range of the change and certain changes to the
            // list have occurred.
            final int currentIndex = getIndex();
            final ListView<T> lv = getListView();
            final List<T> items = lv == null ? null : lv.getItems();
            final int itemCount = items == null ? 0 : items.size();

            final boolean indexAfterChangeFromIndex = currentIndex >= c.getFrom();
            final boolean indexBeforeChangeToIndex = currentIndex < c.getTo() || currentIndex == itemCount;
            final boolean indexInRange = indexAfterChangeFromIndex && indexBeforeChangeToIndex;

            doUpdate = indexInRange || (indexAfterChangeFromIndex && !c.wasReplaced() && (c.wasRemoved() || c.wasAdded()));
        }

        if (doUpdate) {
            updateItem(-1);
        }
    };

    /**
     * Listens to the items property on the ListView. Whenever the entire list is changed,
     * we have to unhook the weakItemsListener and update the item.
     */
    private final InvalidationListener itemsPropertyListener = new InvalidationListener() {
        private WeakReference<ObservableList<T>> weakItemsRef = new WeakReference<>(null);

        @Override public void invalidated(Observable observable) {
            ObservableList<T> oldItems = weakItemsRef.get();
            if (oldItems != null) {
                oldItems.removeListener(weakItemsListener);
            }

            ListView<T> listView = getListView();
            ObservableList<T> items = listView == null ? null : listView.getItems();
            weakItemsRef = new WeakReference<>(items);

            if (items != null) {
                items.addListener(weakItemsListener);
            }
            updateItem(-1);
        }
    };

    /**
     * Listens to the focus model on the ListView. Whenever the focus model changes,
     * the focused property on the ListCell is updated
     */
    private final InvalidationListener focusedListener = value -> {
        updateFocus();
    };

    /**
     * Listens to the focusModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakFocusedListener and update the focus.
     */
    private final ChangeListener<FocusModel<T>> focusModelPropertyListener = new ChangeListener<>() {
        @Override public void changed(ObservableValue<? extends FocusModel<T>> observable,
                                      FocusModel<T> oldValue,
                                      FocusModel<T> newValue) {
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
    private final WeakListChangeListener<Integer> weakSelectedListener = new WeakListChangeListener<>(selectedListener);
    private final WeakChangeListener<MultipleSelectionModel<T>> weakSelectionModelPropertyListener = new WeakChangeListener<>(selectionModelPropertyListener);
    private final WeakListChangeListener<T> weakItemsListener = new WeakListChangeListener<>(itemsListener);
    private final WeakInvalidationListener weakItemsPropertyListener = new WeakInvalidationListener(itemsPropertyListener);
    private final WeakInvalidationListener weakFocusedListener = new WeakInvalidationListener(focusedListener);
    private final WeakChangeListener<FocusModel<T>> weakFocusModelPropertyListener = new WeakChangeListener<>(focusModelPropertyListener);

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The ListView associated with this Cell.
     */
    private ReadOnlyObjectWrapper<ListView<T>> listView = new ReadOnlyObjectWrapper<>(this, "listView") {
        /**
         * A weak reference to the ListView itself, such that whenever the ...
         */
        private WeakReference<ListView<T>> weakListViewRef = new WeakReference<>(null);

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
                    sm.getSelectedIndices().removeListener(weakSelectedListener);
                }

                // If the old focus model isn't null, unhook it
                final FocusModel<T> fm = oldListView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().removeListener(weakFocusedListener);
                }

                // If the old items isn't null, unhook the listener
                final ObservableList<T> items = oldListView.getItems();
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
                final MultipleSelectionModel<T> sm = currentListView.getSelectionModel();
                if (sm != null) {
                    sm.getSelectedIndices().addListener(weakSelectedListener);
                }

                final FocusModel<T> fm = currentListView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().addListener(weakFocusedListener);
                }

                final ObservableList<T> items = currentListView.getItems();
                if (items != null) {
                    items.addListener(weakItemsListener);
                }

                currentListView.editingIndexProperty().addListener(weakEditingListener);
                currentListView.itemsProperty().addListener(weakItemsPropertyListener);
                currentListView.focusModelProperty().addListener(weakFocusModelPropertyListener);
                currentListView.selectionModelProperty().addListener(weakSelectionModelPropertyListener);

                weakListViewRef = new WeakReference<>(currentListView);
            }

            updateItem(-1);
            updateSelection();
            updateFocus();
            requestLayout();
        }
    };
    private void setListView(ListView<T> value) { listView.set(value); }
    public final ListView<T> getListView() { return listView.get(); }
    public final ReadOnlyObjectProperty<ListView<T>> listViewProperty() { return listView.getReadOnlyProperty(); }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void indexChanged(int oldIndex, int newIndex) {
        super.indexChanged(oldIndex, newIndex);

        if (isEditing() && newIndex == oldIndex) {
            // no-op
            // Fix for RT-31165 - if we (needlessly) update the index whilst the
            // cell is being edited it will no longer be in an editing state.
            // This means that in certain (common) circumstances that it will
            // appear that a cell is uneditable as, despite being clicked, it
            // will not change to the editing state as a layout of VirtualFlow
            // is immediately invoked, which forces all cells to be updated.
        } else {
            updateItem(oldIndex);
            updateSelection();
            updateFocus();
            updateEditing();
        }
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ListCellSkin<>(this);
    }

    /*
     * The layoutChildren() method is overridden to address a specific accessibility issue: JDK-8309374
     * If the Accessibility client application requests FOCUS_ITEM before the focused
     * ListViewSkin/ListCell is created, then JavaFX would return a null object,
     * and hence accessibility client application cannot draw the focus rectangle.
     * In this scenario, JavaFX should notify the accessibility application once the
     * focused ListCell is created and its layout is completed.
     */
    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        super.layoutChildren();

        if (isFocused()) {
            ListView<T> listView = getListView();
            if (listView != null) {
                /*
                 * The notifyAccessibleAttributeChanged() call is submitted via runLater to defer it until after
                 * the layout completes, because:
                 * It is possible that when accessibility client application is processing a FOCUS_ITEM notification,
                 * it may trigger a layout of focused ListItem, and it ends up generating another FOCUS_ITEM change
                 * notification from here.
                 * We observed that this scenario occurs when client application is trying to get the
                 * the focus item property of a focused ListItem's parent(ListView).
                 * This scenario is avoided by submitting the call via runLater,
                 * so that the notification is not sent during any getAttribute() call.
                 */
                Platform.runLater(() -> listView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM));
            }
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Editing API                                                             *
     *                                                                         *
     **************************************************************************/
    // index at time of startEdit - fix for JDK-8165214
    private int indexAtStartEdit;

    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (isEditing()) return;
        final ListView<T> list = getListView();
        if (!isEditable() || (list != null && ! list.isEditable())) {
            return;
        }

        // it makes sense to get the cell into its editing state before firing
        // the event to the ListView below, so that's what we're doing here
        // by calling super.startEdit().
        super.startEdit();

        if (!isEditing()) return;

        indexAtStartEdit = getIndex();
         // Inform the ListView of the edit starting.
        if (list != null) {
            list.fireEvent(new ListView.EditEvent<>(list,
                    ListView.<T>editStartEvent(),
                    null,
                    indexAtStartEdit));
            list.edit(indexAtStartEdit);
            list.requestFocus();
        }

    }

    /** {@inheritDoc} */
    @Override public void commitEdit(T newValue) {
        if (! isEditing()) return;

        // inform parent classes of the commit, so that they can switch us
        // out of the editing state.
        // This MUST come before the updateItem call below, otherwise it will
        // call cancelEdit(), resulting in both commit and cancel events being
        // fired (as identified in RT-29650)
        super.commitEdit(newValue);

        ListView<T> list = getListView();
        boolean listShouldRequestFocus = false;

        // JDK-8187307: fire the commit after updating cell's editing state
        if (list != null) {
            // The cell is going to be updated, and the current focus owner might be removed from it.
            // Before that happens, check if it has the list as a parent (otherwise the user might have
            // clicked out of the list entirely and given focus to something else), so the list can
            // request the focus back, once the edit commit ends.
            listShouldRequestFocus = ControlUtils.controlShouldRequestFocusIfCurrentFocusOwnerIsChild(list);

            // Inform the ListView of the edit being ready to be committed.
            list.fireEvent(new ListView.EditEvent<>(list,
                    ListView.<T>editCommitEvent(),
                    newValue,
                    list.getEditingIndex()));
        }

        // Update the item within this cell, so that it represents the new value
        updateItem(-1);

        if (list != null) {
            // reset the editing index on the ListView. This must come after the
            // event is fired so that the developer on the other side can consult
            // the ListView editingIndex property (if they choose to do that
            // rather than just grab the int from the event).
            list.edit(-1);

            // request focus back onto the list, only if the current focus
            // owner had the list as a parent.
            // It would be rude of us to request it back again.
            if (listShouldRequestFocus) {
                list.requestFocus();
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;

        super.cancelEdit();

        // Inform the ListView of the edit being cancelled.
        ListView<T> list = getListView();
        if (list != null) {

            // reset the editing index on the ListView
            if (updateEditingIndex) list.edit(-1);

            // request focus back onto the list, only if the current focus
            // owner has the list as a parent (otherwise the user might have
            // clicked out of the list entirely and given focus to something else).
            // It would be rude of us to request it back again.
            ControlUtils.requestFocusOnControlOnlyIfCurrentFocusOwnerIsChild(list);

            list.fireEvent(new ListView.EditEvent<>(list,
                    ListView.<T>editCancelEvent(),
                    null,
                    indexAtStartEdit));
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private boolean firstRun = true;
    private void updateItem(int oldIndex) {
        final ListView<T> lv = getListView();
        final List<T> items = lv == null ? null : lv.getItems();
        final int index = getIndex();
        final int itemCount = items == null ? -1 : items.size();

        // Compute whether the index for this cell is for a real item
        boolean valid = items != null && index >=0 && index < itemCount;

        final T oldValue = getItem();
        final boolean isEmpty = isEmpty();

        // Cause the cell to update itself
        outer: if (valid) {
            final T newValue = items.get(index);

            // RT-35864 - if the index didn't change, then avoid calling updateItem
            // unless the item has changed.
            if (oldIndex == index) {
                if (!isItemChanged(oldValue, newValue)) {
                    // RT-37054:  we break out of the if/else code here and
                    // proceed with the code following this, so that we may
                    // still update references, listeners, etc as required.
                    break outer;
                }
            }
            updateItem(newValue, false);
        } else {
            // RT-30484 We need to allow a first run to be special-cased to allow
            // for the updateItem method to be called at least once to allow for
            // the correct visual state to be set up. In particular, in RT-30484
            // refer to Ensemble8PopUpTree.png - in this case the arrows are being
            // shown as the new cells are instantiated with the arrows in the
            // children list, and are only hidden in updateItem.
            if (!isEmpty || firstRun) {
                updateItem(null, true);
                firstRun = false;
            }
        }
    }

    /**
     * Updates the ListView associated with this Cell.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins. It is not common
     *       for developers or designers to access this function directly.
     * @param listView the ListView associated with this cell
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
        if (sm == null) {
            updateSelected(false);
            return;
        }

        boolean isSelected = sm.isSelected(index);
        if (isSelected() == isSelected) return;

        updateSelected(isSelected);
    }

    private void updateFocus() {
        int index = getIndex();
        ListView<T> listView = getListView();
        if (index == -1 || listView == null) return;

        FocusModel<T> fm = listView.getFocusModel();
        if (fm == null) {
            setFocused(false);
            return;
        }

        setFocused(fm.isFocused(index));
    }

    private void updateEditing() {
        final int index = getIndex();
        final ListView<T> list = getListView();
        final int editIndex = list == null ? -1 : list.getEditingIndex();
        final boolean editing = isEditing();
        final boolean match = (list != null) && (index != -1) && (index == editIndex);

        if (match && !editing) {
            startEdit();
        } else if (!match && editing) {
            // If my index is not the one being edited then I need to cancel
            // the edit. The tricky thing here is that as part of this call
            // I cannot end up calling list.edit(-1) the way that the standard
            // cancelEdit method would do. Yet, I need to call cancelEdit
            // so that subclasses which override cancelEdit can execute. So,
            // I have to use a kind of hacky flag workaround.
            try {
                // try-finally to make certain that the flag is reliably reset to true
                updateEditingIndex = false;
                cancelEdit();
            } finally {
                updateEditingIndex = true;
            }
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "list-cell";



    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case INDEX: return getIndex();
            case SELECTED: return isSelected();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case REQUEST_FOCUS: {
                ListView<T> listView = getListView();
                if (listView != null) {
                    FocusModel<T> fm = listView.getFocusModel();
                    if (fm != null) {
                        fm.focus(getIndex());
                    }
                }
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }
}


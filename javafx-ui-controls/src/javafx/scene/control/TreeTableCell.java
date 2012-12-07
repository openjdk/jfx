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

import com.sun.javafx.scene.control.skin.TreeTableCellSkin;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;

import javafx.collections.WeakListChangeListener;
import java.lang.ref.WeakReference;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import javafx.scene.control.TreeTableColumn.CellEditEvent;


/**
 * Represents a single row/column intersection in a {@link TreeTableView}. To 
 * represent this intersection, a TreeTableCell contains an 
 * {@link #indexProperty() index} property, as well as a 
 * {@link #tableColumnProperty() tableColumn} property. In addition, a TreeTableCell
 * instance knows what {@link TreeTableRow} it exists in.
 * 
 * @see TreeTableView
 * @see TreeTableColumn
 * @see Cell
 * @see IndexedCell
 * @see TreeTableRow
 * @param <T> The type of the item contained within the Cell.
 */
public class TreeTableCell<S,T> extends IndexedCell<T> {
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a default TreeTableCell instance with a style class of
     * 'tree-table-cell'.
     */
    public TreeTableCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        
        updateColumnIndex();
    }



    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private boolean itemDirty = false;
    
    /*
     * This is the list observer we use to keep an eye on the SelectedCells
     * ObservableList in the tree table view. Because it is possible that the table can
     * be mutated, we create this observer here, and add/remove it from the
     * storeTableView method.
     */
    private ListChangeListener<TablePosition> selectedListener = new ListChangeListener<TablePosition>() {
        @Override public void onChanged(Change<? extends TablePosition> c) {
            updateSelection();
        }
    };

    // same as above, but for focus
    private final InvalidationListener focusedListener = new InvalidationListener() {
        @Override public void invalidated(Observable value) {
            updateFocus();
        }
    };

    // same as above, but for for changes to the properties on TableRow
    private final InvalidationListener tableRowUpdateObserver = new InvalidationListener() {
        @Override public void invalidated(Observable value) {
            itemDirty = true;
            requestLayout();
        }
    };
    
    private final InvalidationListener editingListener = new InvalidationListener() {
        @Override public void invalidated(Observable value) {
            updateEditing();
        }
    };
    
    private ListChangeListener<TableColumn<S,T>> visibleLeafColumnsListener = new ListChangeListener<TableColumn<S,T>>() {
        @Override public void onChanged(Change<? extends TableColumn<S,T>> c) {
            updateColumnIndex();
        }
    };
    
    private final WeakListChangeListener weakSelectedListener = 
            new WeakListChangeListener(selectedListener);
    private final WeakInvalidationListener weakFocusedListener = 
            new WeakInvalidationListener(focusedListener);
    private final WeakInvalidationListener weaktableRowUpdateObserver = 
            new WeakInvalidationListener(tableRowUpdateObserver);
    private final WeakInvalidationListener weakEditingListener = 
            new WeakInvalidationListener(editingListener);
    private final WeakListChangeListener weakVisibleLeafColumnsListener =
            new WeakListChangeListener(visibleLeafColumnsListener);

    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    // --- TableColumn
    /**
     * The TreeTableColumn instance that backs this TreeTableCell.
     */
    private ReadOnlyObjectWrapper<TreeTableColumn<S,T>> treeTableColumn = 
            new ReadOnlyObjectWrapper<TreeTableColumn<S,T>>(this, "treeTableColumn") {
        @Override protected void invalidated() {
            updateColumnIndex();
        }
    };
    public final ReadOnlyObjectProperty<TreeTableColumn<S,T>> tableColumnProperty() { return treeTableColumn.getReadOnlyProperty(); }
    private void setTableColumn(TreeTableColumn<S,T> value) { treeTableColumn.set(value); }
    public final TreeTableColumn<S,T> getTableColumn() { return treeTableColumn.get(); }
    
    
    // --- TableView
    /**
     * The TreeTableView associated with this TreeTableCell.
     */
    private ReadOnlyObjectWrapper<TreeTableView<S>> treeTableView;
    private void setTreeTableView(TreeTableView<S> value) {
        treeTableViewPropertyImpl().set(value);
    }
    public final TreeTableView<S> getTreeTableView() {
        return treeTableView == null ? null : treeTableView.get();
    }
    public final ReadOnlyObjectProperty<TreeTableView<S>> treeTableViewProperty() {
        return treeTableViewPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TreeTableView<S>> treeTableViewPropertyImpl() {
        if (treeTableView == null) {
            treeTableView = new ReadOnlyObjectWrapper<TreeTableView<S>>(this, "treeTableView") {
                private WeakReference<TreeTableView<S>> weakTableViewRef;
                @Override protected void invalidated() {
                    TreeTableView.TreeTableViewSelectionModel sm;
                    TreeTableView.TreeTableViewFocusModel fm;
                    
                    if (weakTableViewRef != null) {
                        TreeTableView<S> oldTableView = weakTableViewRef.get();
                        if (oldTableView != null) {
                            sm = oldTableView.getSelectionModel();
                            if (sm != null) {
                                sm.getSelectedCells().removeListener(weakSelectedListener);
                            }

                            fm = oldTableView.getFocusModel();
                            if (fm != null) {
                                fm.focusedCellProperty().removeListener(weakFocusedListener);
                            }

                            oldTableView.editingCellProperty().removeListener(weakEditingListener);
                            oldTableView.getVisibleLeafColumns().removeListener(weakVisibleLeafColumnsListener);
                        }
                    }
                    
                    if (get() != null) {
                        sm = get().getSelectionModel();
                        if (sm != null) {
                            sm.getSelectedCells().addListener(weakSelectedListener);
                        }

                        fm = get().getFocusModel();
                        if (fm != null) {
                            fm.focusedCellProperty().addListener(weakFocusedListener);
                        }

                        get().editingCellProperty().addListener(weakEditingListener);
                        get().getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);
                        
                        weakTableViewRef = new WeakReference<TreeTableView<S>>(get());
                    }
                    
                    updateColumnIndex();
                }
            };
        }
        return treeTableView;
    }
    
    
    // --- TableRow
    /**
     * The TreeTableRow that this TreeTableCell currently finds itself placed within.
     */
    private ReadOnlyObjectWrapper<TreeTableRow> treeTableRow = 
            new ReadOnlyObjectWrapper<TreeTableRow>(this, "treeTableRow");
    private void setTreeTableRow(TreeTableRow value) { treeTableRow.set(value); }
    public final TreeTableRow getTreeTableRow() { return treeTableRow.get(); }
    public final ReadOnlyObjectProperty<TreeTableRow> tableRowProperty() { return treeTableRow;  }



    /***************************************************************************
     *                                                                         *
     * Editing API                                                             *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        final TreeTableView table = getTreeTableView();
        final TreeTableColumn column = getTableColumn();
        if (! isEditable() ||
                (table != null && ! table.isEditable()) ||
                (column != null && ! getTableColumn().isEditable())) {
            return;
        }
        
        // it makes sense to get the cell into its editing state before firing
        // the event to listeners below, so that's what we're doing here
        // by calling super.startEdit().
        super.startEdit();
        
        if (column != null) {
            CellEditEvent editEvent = new CellEditEvent(
                table,
                table.getEditingCell(),
                TableColumn.editStartEvent(),
                null
            );

            Event.fireEvent(column, editEvent);
        }
    }

    /** {@inheritDoc} */
    @Override public void commitEdit(T newValue) {
        if (! isEditing()) return;
        
        final TreeTableView table = getTreeTableView();
        if (table != null) {
            // Inform the TableView of the edit being ready to be committed.
            CellEditEvent editEvent = new CellEditEvent(
                table,
                table.getEditingCell(),
                TableColumn.editCommitEvent(),
                newValue
            );

            Event.fireEvent(getTableColumn(), editEvent);
        }

        // update the item within this cell, so that it represents the new value
        updateItem(newValue, false);

        // inform parent classes of the commit, so that they can switch us
        // out of the editing state
        super.commitEdit(newValue);
        
        if (table != null) {
            // reset the editing cell on the TableView
            table.edit(-1, null);
            table.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        if (! isEditing()) return;

        final TreeTableView table = getTreeTableView();

        super.cancelEdit();

        // reset the editing index on the TableView
        if (table != null) {
            TreeTablePosition editingCell = table.getEditingCell();
            if (updateEditingIndex) table.edit(-1, null);

            CellEditEvent editEvent = new CellEditEvent(
                table,
                editingCell,
                TableColumn.editCancelEvent(),
                null
            );

            Event.fireEvent(getTableColumn(), editEvent);
        }
    }
    
    
    
    /* *************************************************************************
     *                                                                         *
     * Overriding methods                                                      *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void updateSelected(boolean selected) {
        // copied from Cell, with the first conditional clause below commented 
        // out, as it is valid for an empty TableCell to be selected, as long 
        // as the parent TableRow is not empty (see RT-15529).
        /*if (selected && isEmpty()) return;*/
        if (getTreeTableRow() == null || getTreeTableRow().isEmpty()) return;
        setSelected(selected);
    }

    

    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/
    
    @Override void indexChanged() {
        super.indexChanged();
        // Ideally we would just use the following two lines of code, rather
        // than the updateItem() call beneath, but if we do this we end up with
        // RT-22428 where all the columns are collapsed.
        // itemDirty = true;
        // requestLayout();
        updateItem();
        updateSelection();
        updateFocus();
    }
    
    private boolean isLastVisibleColumn = false;
    private int columnIndex = -1;
    
    private void updateColumnIndex() {
        final TreeTableView tv = getTreeTableView();
        TreeTableColumn tc = getTableColumn();
        columnIndex = tv == null || tc == null ? -1 : tv.getVisibleLeafIndex(tc);
        
        // update the pseudo class state regarding whether this is the last
        // visible cell (i.e. the right-most). 
        boolean old = isLastVisibleColumn;
        isLastVisibleColumn = getTableColumn() != null &&
                columnIndex != -1 && 
                columnIndex == tv.getVisibleLeafColumns().size() - 1;
        if (old != isLastVisibleColumn) {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_LAST_VISIBLE);
        }
    }

    private void updateSelection() {
        /*
         * This cell should be selected if the selection mode of the table
         * is cell-based, and if the row and column that this cell represents
         * is selected.
         *
         * If the selection mode is not cell-based, then the listener in the
         * TableRow class might pick up the need to set an entire row to be
         * selected.
         */
        if (isEmpty()) return;
        
        final TreeTableView tv = getTreeTableView();
        if (getIndex() == -1 || getTreeTableView() == null) return;
        if (tv.getSelectionModel() == null) return;
        
        boolean isSelected = isInCellSelectionMode() &&
                tv.getSelectionModel().isSelected(getIndex(), getTableColumn());
        if (isSelected() == isSelected) return;

        updateSelected(isSelected);
    }

    private void updateFocus() {
        final TreeTableView tv = getTreeTableView();
        if (getIndex() == -1 || tv == null) return;
        if (tv.getFocusModel() == null) return;
        
        boolean isFocused = isInCellSelectionMode() &&
                tv.getFocusModel() != null &&
                tv.getFocusModel().isFocused(getIndex(), getTableColumn());

        setFocused(isFocused);
    }

    private void updateEditing() {
        final TreeTableView tv = getTreeTableView();
        if (getIndex() == -1 || tv == null) return;

        TreeTablePosition editCell = tv.getEditingCell();
        boolean match = match(editCell);
        
        if (match && ! isEditing()) {
            startEdit();
        } else if (! match && isEditing()) {
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
    private boolean updateEditingIndex = true;

    private boolean match(TreeTablePosition pos) {
        return pos != null && pos.getRow() == getIndex() && pos.getTableColumn() == getTableColumn();
    }

    private boolean isInCellSelectionMode() {
        TreeTableView treeTable = getTreeTableView();
        return treeTable != null &&
                treeTable.getSelectionModel() != null &&
                treeTable.getSelectionModel().isCellSelectionEnabled();
    }
    
    /*
     * This was brought in to fix the issue in RT-22077, namely that the 
     * ObservableValue was being GC'd, meaning that changes to the value were
     * no longer being delivered. By extracting this value out of the method, 
     * it is now referred to from TableCell and will therefore no longer be
     * GC'd.
     */
    private ObservableValue<T> currentObservableValue = null;

    /*
     * This is called when we think that the data within this TreeTableCell may have
     * changed. You'll note that this is a private function - it is only called
     * when one of the triggers above call it.
     */
    private void updateItem() {
        if (currentObservableValue != null) {
            currentObservableValue.removeListener(weaktableRowUpdateObserver);
        }
        
        // there is a whole heap of reasons why we should just punt...
        if (getIndex() < 0 || 
                columnIndex < 0 ||
                !isVisible() ||
                getTableColumn() == null || 
                !getTableColumn().isVisible() || 
                getTreeTableView().getRoot() == null) {
            return;
        }
        
        // get the total number of items in the data model
        int itemCount = getTreeTableView().impl_getTreeItemCount();
        
        if (getIndex() >= itemCount) {
            updateItem(null, true);
            return;
        } else {
            if (getIndex() < itemCount) {
                currentObservableValue = getTableColumn().getCellObservableValue(getIndex());
            }

            T value = currentObservableValue == null ? null : currentObservableValue.getValue();
            
            // update the 'item' property of this cell.
            updateItem(value, value == null);
        }
        
        if (currentObservableValue == null) {
            return;
        }
        
        // add property change listeners to this item
        currentObservableValue.addListener(weaktableRowUpdateObserver);
    }

    @Override protected void layoutChildren() {
        if (itemDirty) {
            updateItem();
            itemDirty = false;
        }
        super.layoutChildren();
    }

    


    /***************************************************************************
     *                                                                         *
     *                              Expert API                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Updates the TreeTableView associated with this TreeTableCell. This is typically
     * only done once when the TreeTableCell is first added to the TreeTableView.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTreeTableView(TreeTableView tv) {
        setTreeTableView(tv);
    }

    /**
     * Updates the TreeTableRow associated with this TreeTableCell.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTreeTableRow(TreeTableRow treeTableRow) {
        this.setTreeTableRow(treeTableRow);
    }

    /**
     * Updates the TreeTableColumn associated with this TreeTableCell.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTreeTableColumn(TreeTableColumn col) {
        setTableColumn(col);
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-table-cell";
    private static final String PSEUDO_CLASS_LAST_VISIBLE = "last-visible";

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeTableCellSkin(this);
    }
}

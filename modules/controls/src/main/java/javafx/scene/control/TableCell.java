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

import javafx.css.PseudoClass;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.control.TableView.TableViewFocusModel;

import com.sun.javafx.scene.control.skin.TableCellSkin;
import javafx.collections.WeakListChangeListener;
import java.lang.ref.WeakReference;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;

import javafx.scene.control.TableColumn.CellEditEvent;


/**
 * Represents a single row/column intersection in a {@link TableView}. To 
 * represent this intersection, a TableCell contains an 
 * {@link #indexProperty() index} property, as well as a 
 * {@link #tableColumnProperty() tableColumn} property. In addition, a TableCell
 * instance knows what {@link TableRow} it exists in.
 * 
 * @see TableView
 * @see TableColumn
 * @see Cell
 * @see IndexedCell
 * @see TableRow
 * @param <T> The type of the item contained within the Cell.
 * @since JavaFX 2.0
 */
public class TableCell<S,T> extends IndexedCell<T> {
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a default TableCell instance with a style class of 'table-cell'
     */
    public TableCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        
        updateColumnIndex();
    }



    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    // package for testing
    boolean lockItemOnEdit = false;


    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private boolean itemDirty = false;
    
    /*
     * This is the list observer we use to keep an eye on the SelectedCells
     * ObservableList in the table view. Because it is possible that the table can
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
    
    private ListChangeListener<TableColumn<S,?>> visibleLeafColumnsListener = new ListChangeListener<TableColumn<S,?>>() {
        @Override public void onChanged(Change<? extends TableColumn<S,?>> c) {
            updateColumnIndex();
        }
    };
    
    private ListChangeListener<String> columnStyleClassListener = new ListChangeListener<String>() {
        @Override public void onChanged(Change<? extends String> c) {
            while (c.next()) {
                if (c.wasRemoved()) {
                    getStyleClass().removeAll(c.getRemoved());
                }
                
                if (c.wasAdded()) {
                    getStyleClass().addAll(c.getAddedSubList());
                }
            }
        }
    };
    
    private final WeakListChangeListener<TablePosition> weakSelectedListener =
            new WeakListChangeListener<>(selectedListener);
    private final WeakInvalidationListener weakFocusedListener = 
            new WeakInvalidationListener(focusedListener);
    private final WeakInvalidationListener weaktableRowUpdateObserver = 
            new WeakInvalidationListener(tableRowUpdateObserver);
    private final WeakInvalidationListener weakEditingListener = 
            new WeakInvalidationListener(editingListener);
    private final WeakListChangeListener<TableColumn<S,?>> weakVisibleLeafColumnsListener =
            new WeakListChangeListener<>(visibleLeafColumnsListener);
    private final WeakListChangeListener<String> weakColumnStyleClassListener =
            new WeakListChangeListener<String>(columnStyleClassListener);

    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    // --- TableColumn
    private ReadOnlyObjectWrapper<TableColumn<S,T>> tableColumn = new ReadOnlyObjectWrapper<TableColumn<S,T>>() {
        @Override protected void invalidated() {
            updateColumnIndex();
        }

        @Override public Object getBean() {
            return TableCell.this;
        }

        @Override public String getName() {
            return "tableColumn";
        }
    };
    /**
     * The TableColumn instance that backs this TableCell.
     */
    public final ReadOnlyObjectProperty<TableColumn<S,T>> tableColumnProperty() { return tableColumn.getReadOnlyProperty(); }
    private void setTableColumn(TableColumn<S,T> value) { tableColumn.set(value); }
    public final TableColumn<S,T> getTableColumn() { return tableColumn.get(); }
    
    
    // --- TableView
    private ReadOnlyObjectWrapper<TableView<S>> tableView;
    private void setTableView(TableView<S> value) {
        tableViewPropertyImpl().set(value);
    }
    public final TableView<S> getTableView() {
        return tableView == null ? null : tableView.get();
    }

    /**
     * The TableView associated with this TableCell.
     */
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() {
        return tableViewPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TableView<S>> tableViewPropertyImpl() {
        if (tableView == null) {
            tableView = new ReadOnlyObjectWrapper<TableView<S>>() {
                private WeakReference<TableView<S>> weakTableViewRef;
                @Override protected void invalidated() {
                    TableView.TableViewSelectionModel<S> sm;
                    TableViewFocusModel<S> fm;
                    
                    if (weakTableViewRef != null) {
                        cleanUpTableViewListeners(weakTableViewRef.get());
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
                        
                        weakTableViewRef = new WeakReference<TableView<S>>(get());
                    }
                    
                    updateColumnIndex();
                }

                @Override public Object getBean() {
                    return TableCell.this;
                }

                @Override public String getName() {
                    return "tableView";
                }
            };
        }
        return tableView;
    }



    // --- TableRow
    /**
     * The TableRow that this TableCell currently finds itself placed within.
     */
    private ReadOnlyObjectWrapper<TableRow> tableRow = new ReadOnlyObjectWrapper<TableRow>(this, "tableRow");
    private void setTableRow(TableRow value) { tableRow.set(value); }
    public final TableRow getTableRow() { return tableRow.get(); }
    public final ReadOnlyObjectProperty<TableRow> tableRowProperty() { return tableRow;  }



    /***************************************************************************
     *                                                                         *
     * Editing API                                                             *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        final TableView table = getTableView();
        final TableColumn column = getTableColumn();
        if (! isEditable() ||
                (table != null && ! table.isEditable()) ||
                (column != null && ! getTableColumn().isEditable())) {
            return;
        }

        // We check the boolean lockItemOnEdit field here, as whilst we want to
        // updateItem normally, when it comes to unit tests we can't have the
        // item change in all circumstances.
        if (! lockItemOnEdit) {
            updateItem();
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
        
        final TableView table = getTableView();
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

        final TableView table = getTableView();

        super.cancelEdit();

        // reset the editing index on the TableView
        if (table != null) {
            TablePosition editingCell = table.getEditingCell();
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
        if (getTableRow() == null || getTableRow().isEmpty()) return;
        setSelected(selected);
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TableCellSkin(this);
    }
    
//    @Override public void dispose() {
//        cleanUpTableViewListeners(getTableView());
//        
//        if (currentObservableValue != null) {
//            currentObservableValue.removeListener(weaktableRowUpdateObserver);
//        }
//        
//        super.dispose();
//    }

    /* *************************************************************************
    *                                                                         *
    * Private Implementation                                                  *
    *                                                                         *
    **************************************************************************/
    
    private void cleanUpTableViewListeners(TableView<S> tableView) {
        if (tableView != null) {
            TableView.TableViewSelectionModel sm = tableView.getSelectionModel();
            if (sm != null) {
                sm.getSelectedCells().removeListener(weakSelectedListener);
            }

            TableViewFocusModel fm = tableView.getFocusModel();
            if (fm != null) {
                fm.focusedCellProperty().removeListener(weakFocusedListener);
            }

            tableView.editingCellProperty().removeListener(weakEditingListener);
            tableView.getVisibleLeafColumns().removeListener(weakVisibleLeafColumnsListener);
        }        
    }
    
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
        TableView tv = getTableView();
        TableColumn tc = getTableColumn();
        columnIndex = tv == null || tc == null ? -1 : tv.getVisibleLeafIndex(tc);
        
        // update the pseudo class state regarding whether this is the last
        // visible cell (i.e. the right-most). 
        isLastVisibleColumn = getTableColumn() != null &&
                columnIndex != -1 && 
                columnIndex == getTableView().getVisibleLeafColumns().size() - 1;
        pseudoClassStateChanged(PSEUDO_CLASS_LAST_VISIBLE, isLastVisibleColumn);
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
        if (getIndex() == -1 || getTableView() == null) return;
        if (getTableView().getSelectionModel() == null) return;
        
        boolean isSelected = isInCellSelectionMode() &&
                getTableView().getSelectionModel().isSelected(getIndex(), getTableColumn());
        if (isSelected() == isSelected) return;

        updateSelected(isSelected);
    }

    private void updateFocus() {
        if (getIndex() == -1 || getTableView() == null) return;
        if (getTableView().getFocusModel() == null) return;
        
        boolean isFocused = isInCellSelectionMode() &&
                getTableView().getFocusModel() != null &&
                getTableView().getFocusModel().isFocused(getIndex(), getTableColumn());

        setFocused(isFocused);
    }

    private void updateEditing() {
        if (getIndex() == -1 || getTableView() == null) return;

        TablePosition editCell = getTableView().getEditingCell();
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

    private boolean match(TablePosition pos) {
        return pos != null && pos.getRow() == getIndex() && pos.getTableColumn() == getTableColumn();
    }

    private boolean isInCellSelectionMode() {
        return getTableView() != null &&
                getTableView().getSelectionModel() != null &&
                getTableView().getSelectionModel().isCellSelectionEnabled();
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
     * This is called when we think that the data within this TableCell may have
     * changed. You'll note that this is a private function - it is only called
     * when one of the triggers above call it.
     */
    private void updateItem() {
        if (currentObservableValue != null) {
            currentObservableValue.removeListener(weaktableRowUpdateObserver);
        }
        
        // get the total number of items in the data model
        final TableView tableView = getTableView();
        final List<T> items = tableView == null ? FXCollections.<T>emptyObservableList() : tableView.getItems();
        final TableColumn tableColumn = getTableColumn();
        final int itemCount = items.size();
        final int index = getIndex();
        final boolean isEmpty = isEmpty();
        final T oldValue = getItem();
        
        // there is a whole heap of reasons why we should just punt...
        if (index >= itemCount ||
                index < 0 || 
                columnIndex < 0 ||
                !isVisible() ||
                tableColumn == null || 
                !tableColumn.isVisible()) {

            if (!isEmpty && oldValue != null) {
                updateItem(null, true);
            }
            return;
        } else {
            currentObservableValue = tableColumn.getCellObservableValue(index);
            final T newValue = currentObservableValue == null ? null : currentObservableValue.getValue();

            if ((newValue != null && ! newValue.equals(oldValue)) ||
                    oldValue != null && ! oldValue.equals(newValue)) {
                updateItem(newValue, false);
            } else if(isEmpty && newValue == null) {
                updateItem(newValue, false);
            }
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
     * Updates the TableView associated with this TableCell. This is typically
     * only done once when the TableCell is first added to the TableView.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTableView(TableView tv) {
        setTableView(tv);
    }

    /**
     * Updates the TableRow associated with this TableCell.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTableRow(TableRow tableRow) {
        this.setTableRow(tableRow);
    }

    /**
     * Updates the TableColumn associated with this TableCell.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins. It is not common
     *         for developers or designers to access this function directly.
     */
    public final void updateTableColumn(TableColumn col) {
        // remove style class of existing table column, if it is non-null
        TableColumn<S,T> oldCol = getTableColumn();
        if (oldCol != null) {
            oldCol.getStyleClass().removeListener(weakColumnStyleClassListener);
            getStyleClass().removeAll(oldCol.getStyleClass());
        }
        
        setTableColumn(col);
        
        if (col != null) {
            getStyleClass().addAll(col.getStyleClass());
            col.getStyleClass().addListener(weakColumnStyleClassListener);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "table-cell";
    private static final PseudoClass PSEUDO_CLASS_LAST_VISIBLE = 
            PseudoClass.getPseudoClass("last-visible");

}

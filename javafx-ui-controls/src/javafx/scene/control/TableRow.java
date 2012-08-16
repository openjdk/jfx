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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView.TableViewFocusModel;

import com.sun.javafx.scene.control.WeakListChangeListener;
import java.lang.ref.WeakReference;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * <p>TableRow is an {@link javafx.scene.control.IndexedCell IndexedCell}, but
 * rarely needs to be used by developers creating TableView instances. The only
 * time TableRow is likely to be encountered at all by a developer is if they
 * wish to create a custom {@link TableView#rowFactoryProperty() rowFactory} 
 * that replaces an entire row of a TableView.</p>
 *
 * <p>More often than not, it is actually easier for a developer to customize
 * individual cells in a row, rather than the whole row itself. To do this,
 * you can specify a custom {@link TableColumn#cellFactoryProperty() cellFactory} 
 * on each TableColumn instance.</p>
 *
 * @see TableView
 * @see TableColumn
 * @see TableCell
 * @see IndexedCell
 * @see Cell
 * @param <T> The type of the item contained within the Cell.
 */
public class TableRow<T> extends IndexedCell<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a default TableRow instance with a style class of 'table-row-cell'
     */
    public TableRow() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    

    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/

    /*
     * This is the list observer we use to keep an eye on the SelectedCells
     * list in the table view. Because it is possible that the table can
     * be mutated, we create this observer here, and add/remove it from the
     * storeTableView method.
     */
    private ListChangeListener<TablePosition> selectedListener = new ListChangeListener<TablePosition>() {
        @Override
        public void onChanged(Change<? extends TablePosition> c) {
            updateSelection();
        }
    };

    // Same as selectedListener, but this time for focus events
    private final InvalidationListener focusedListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateFocus();
        }
    };

    // same as above, but for editing events
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
    
    // --- TableView
    private ReadOnlyObjectWrapper<TableView<T>> tableView;
    private void setTableView(TableView<T> value) {
        tableViewPropertyImpl().set(value);
    }

    public final TableView<T> getTableView() {
        return tableView == null ? null : tableView.get();
    }

    /**
     * The TableView associated with this Cell.
     */
    public final ReadOnlyObjectProperty<TableView<T>> tableViewProperty() {
        return tableViewPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TableView<T>> tableViewPropertyImpl() {
        if (tableView == null) {
            tableView = new ReadOnlyObjectWrapper<TableView<T>>() {
                private WeakReference<TableView<T>> weakTableViewRef;
                @Override protected void invalidated() {
                    TableView.TableViewSelectionModel sm;
                    TableViewFocusModel fm;

                    if (weakTableViewRef != null) {
                        TableView oldTableView = weakTableViewRef.get();
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
                        }
                        
                        weakTableViewRef = null;
                    }

                    if (getTableView() != null) {
                        sm = getTableView().getSelectionModel();
                        if (sm != null) {
                            sm.getSelectedCells().addListener(weakSelectedListener);
                        }

                        fm = getTableView().getFocusModel();
                        if (fm != null) {
                            fm.focusedCellProperty().addListener(weakFocusedListener);
                        }

                        getTableView().editingCellProperty().addListener(weakEditingListener);
                        
                        weakTableViewRef = new WeakReference<TableView<T>>(get());
                    }
                }

                @Override
                public Object getBean() {
                    return TableRow.this;
                }

                @Override
                public String getName() {
                    return "tableView";
                }
            };
        }
        return tableView;
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void indexChanged() {
        super.indexChanged();
        updateItem();
        updateSelection();
        updateFocus();
    }
    
    private void updateItem() {
        TableView<T> tv = getTableView();
        if (tv == null || tv.getItems() == null) return;
        
        List<T> items = tv.getItems();

        // Compute whether the index for this cell is for a real item
        boolean valid = getIndex() >= 0 && getIndex() < items.size();

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
    
    private void updateSelection() {
        /*
         * This cell should be selected if the selection mode of the table
         * is row-based, and if the row that this cell represents is selected.
         *
         * If the selection mode is not row-based, then the listener in the
         * TableCell class might pick up the need to set a single cell to be
         * selected.
         */
        if (getIndex() == -1) return;
        
        TableView<T> table = getTableView();
        boolean isSelected = table != null &&
                table.getSelectionModel() != null &&
                ! table.getSelectionModel().isCellSelectionEnabled() &&
                table.getSelectionModel().isSelected(getIndex());

        updateSelected(isSelected);
    }

    private void updateFocus() {
        if (getIndex() == -1) return;
        
        TableView<T> table = getTableView();
        TableView.TableViewSelectionModel sm = table.getSelectionModel();
        TableView.TableViewFocusModel fm = table.getFocusModel();
        boolean isFocused = table != null &&
                sm != null &&
                ! sm.isCellSelectionEnabled() &&
                fm != null &&
                fm.isFocused(getIndex());

        setFocused(isFocused);
    }

    private void updateEditing() {
        if (getIndex() == -1) return;
        
        TableView<T> table = getTableView();
        if (table == null) return;

        TableView.TableViewSelectionModel sm = table.getSelectionModel();
        if (sm == null || sm.isCellSelectionEnabled()) return;

        TablePosition editCell = table.getEditingCell();
        boolean rowMatch = editCell.getRow() == getIndex();

        if (! isEditing() && rowMatch) {
            startEdit();
        } else if (isEditing() && ! rowMatch) {
            cancelEdit();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Expert API                                                              *
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
    public final void updateTableView(TableView<T> tv) {
        setTableView(tv);
    }


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "table-row-cell";
}

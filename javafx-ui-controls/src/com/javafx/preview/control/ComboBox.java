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

package com.javafx.preview.control;

import com.sun.javafx.css.StyleManager;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBox<T> extends ComboBoxBase<T> {
    
    public ComboBox() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setItems(FXCollections.<T>observableArrayList());
        setSelectionModel(new ComboBoxSelectionModel<T>(this));
    }
    
    
    // --- string converter
    /**
     * Converts the user-typed input (when the ComboBox is editable) to an
     * object of type T, such that the input may be retrieved via the 
     * {@link #valueProperty() value} property.
     */
    public ObjectProperty<StringConverter<T>> converterProperty() { return converter; }
    private ObjectProperty<StringConverter<T>> converter = 
            new SimpleObjectProperty<StringConverter<T>>(this, "converter", defaultStringConverter());
    public final void setConverter(StringConverter<T> value) { converterProperty().set(value); }
    public final StringConverter<T> getConverter() {return converterProperty().get(); }
    
    
    // --- items
    /**
     * The list of items to show within the ComboBox popup.
     */
    private ObjectProperty<ObservableList<T>> items = 
            new SimpleObjectProperty<ObservableList<T>>(this, "items");
    public final void setItems(ObservableList<T> value) { itemsProperty().set(value); }
    public final ObservableList<T> getItems() {return items.get(); }
    public ObjectProperty<ObservableList<T>> itemsProperty() { return items; }
    
    
    // --- cell factory
    /**
     * Providing a custom cell factory allows for complete customization of the
     * rendering of items in the ComboBox. Refer to the {@link Cell} javadoc
     * for more information on cell factories.
     */
    private ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory = 
            new SimpleObjectProperty<Callback<ListView<T>, ListCell<T>>>(this, "cellFactory");
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> value) { cellFactoryProperty().set(value); }
    public final Callback<ListView<T>, ListCell<T>> getCellFactory() {return cellFactoryProperty().get(); }
    public ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() { return cellFactory; }
    
    
    // --- Selection Model
    /**
     * The selection model for the ComboBox. Generally, only a single choice 
     * can be made in a ComboBox, and therefore implementations of ComboBoxBase
     * will tend to return a {@link SingleSelectionModel} instance. However,
     * because this can not be guaranteed, it is important to confirm the class
     * type if you intend to use API that is not part of {@link SelectionModel}.
     */
    private ObjectProperty<SelectionModel<T>> selectionModel 
            = new SimpleObjectProperty<SelectionModel<T>>(this, "selectionModel");
    protected final void setSelectionModel(SelectionModel<T> value) { selectionModel.set(value); }
    public final SelectionModel<T> getSelectionModel() { return selectionModel.get(); }
    public final ObjectProperty<SelectionModel<T>> selectionModelProperty() { return selectionModel; }
    
    
    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    private StringConverter<T> defaultStringConverter() {
        return new StringConverter<T>() {
            @Override public String toString(T t) {
                return t == null ? "" : t.toString();
            }

            @Override public T fromString(String string) {
                try {
                    return (T) string;
                } catch (ClassCastException e) {
                    String s = "Can not convert user input into generic type. "
                            + "Please provide a custom ComboBox converter to "
                            + "support this form of user input.";
                    throw new RuntimeException(s);
                }
            }
        };
    }
    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "combo-box";
    
    // package for testing
    static class ComboBoxSelectionModel<T> extends SingleSelectionModel<T> {
        private final ComboBox<T> comboBox;

        public ComboBoxSelectionModel(final ComboBox<T> cb) {
            if (cb == null) {
                throw new NullPointerException("ComboBox can not be null");
            }
            this.comboBox = cb;

            /*
             * The following two listeners are used in conjunction with
             * SelectionModel.select(T obj) to allow for a developer to select
             * an item that is not actually in the data model. When this occurs,
             * we actively try to find an index that matches this object, going
             * so far as to actually watch for all changes to the items list,
             * rechecking each time.
             */

            // watching for changes to the items list content
            final ListChangeListener<T> itemsContentObserver = new ListChangeListener<T>() {
                @Override public void onChanged(Change<? extends T> c) {
                    if (getSelectedIndex() == -1 && getSelectedItem() != null) {
                        int newIndex = comboBox.getItems().indexOf(getSelectedItem());
                        if (newIndex != -1) {
                            setSelectedIndex(newIndex);
                        }
                    }
                }
            };
            if (this.comboBox.getItems() != null) {
                this.comboBox.getItems().addListener(itemsContentObserver);
            }

            // watching for changes to the items list
            InvalidationListener itemsObserver = new InvalidationListener() {
                private ObservableList<T> oldList;
                @Override public void invalidated(Observable o) {
                    if (oldList != null) {
                        oldList.removeListener(itemsContentObserver);
                    }
                    this.oldList = cb.getItems();
                    if (oldList != null) {
                        oldList.addListener(itemsContentObserver);
                    }
                }
            };
            this.comboBox.itemsProperty().addListener(itemsObserver);
        }

        // API Implementation
        @Override protected T getModelItem(int index) {
            final ObservableList<T> items = comboBox.getItems();
            if (items == null) return null;
            if (index < 0 || index >= items.size()) return null;
            return items.get(index);
        }

        @Override protected int getItemCount() {
            final ObservableList<T> items = comboBox.getItems();
            return items == null ? 0 : items.size();
        }
    }
}

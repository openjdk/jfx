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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * An implementation of the {@link ComboBoxBase} abstract class for the most common
 * form of ComboBox, where a popup list is shown to users providing them with
 * a choice that they may select from. For more information around the general
 * concepts and API of ComboBox, refer to the {@link ComboBoxBase} class 
 * documentation.
 * 
 * <p>On top of ComboBoxBase, the ComboBox class introduces additional API. Most
 * importantly, it adds an {@link #itemsProperty() items} property that works in
 * much the same way as the ListView {@link ListView#itemsProperty() items}
 * property. In other words, it is the content of the items list that is displayed
 * to users when they click on the ComboBox button.
 * 
 * <p>By default, when the popup list is showing, the maximum number of rows
 * visible is 10, but this can be changed by modifying the 
 * {@link #visibleRowCountProperty() visibleRowCount} property. If the number of
 * items in the ComboBox is less than the value of <code>visibleRowCount</code>,
 * then the items size will be used instead so that the popup list is not
 * exceedingly long.
 * 
 * <p>As with ListView, it is possible to modify the 
 * {@link javafx.scene.control.SelectionModel SelectionModel} that is used, 
 * although this is likely to be rarely changed. The default
 * SelectionModel used in ComboBox is a {@link SingleSelectionModel}, but this
 * can be switched out by developers to instead allow for multiple selection to 
 * occur, or to alter the behavior of the various methods provided in these APIs.
 * 
 * <p>As the ComboBox internally renders content with a ListView, API exists in
 * the ComboBox class to allow for a custom cell factory to be set. For more
 * information on cell factories, refer to the {@link Cell} and {@link ListCell}
 * classes.
 * 
 * <p>Because a ComboBox can be {@link #editableProperty() editable}, and the
 * default means of allowing user input is via a {@link TextField}, a 
 * {@link #converterProperty() string converter} property is provided to allow
 * for developers to specify how to translate a users string into an object of
 * type T, such that the {@link #valueProperty() value} property may contain it.
 * By default the converter simply returns the String input as the user typed it,
 * which therefore assumes that the type of the editable ComboBox is String. If 
 * a different type is specified and the ComboBox is to be editable, it is 
 * necessary to specify a custom {@link StringConverter}.
 * 
 * @see ComboBoxBase
 * @see Cell
 * @see ListCell
 * @see StringConverter
 */
public class ComboBox<T> extends ComboBoxBase<T> {

    /**
     * Creates a default ComboBox instance with an empty 
     * {@link #itemsProperty() items} list and default 
     * {@link #selectionModelProperty() selection model}.
     */
    public ComboBox() {
        this(FXCollections.<T>observableArrayList());
    }
    
    /**
     * Creates a default ComboBox instance with the provided items list and
     * a default {@link #selectionModelProperty() selection model}.
     */
    public ComboBox(ObservableList<T> items) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setItems(items);
        setSelectionModel(new ComboBoxSelectionModel<T>(this));
    }
    
    
    // --- items
    /**
     * The list of items to show within the ComboBox popup.
     */
    private ObjectProperty<ObservableList<T>> items = 
            new SimpleObjectProperty<ObservableList<T>>(this, "items");
    public final void setItems(ObservableList<T> value) { itemsProperty().set(value); }
    public final ObservableList<T> getItems() {return items.get(); }
    public ObjectProperty<ObservableList<T>> itemsProperty() { return items; }
    
    
    // --- string converter
    /**
     * Converts the user-typed input (when the ComboBox is 
     * {@link #editableProperty() editable}) to an object of type T, such that 
     * the input may be retrieved via the  {@link #valueProperty() value} property.
     */
    public ObjectProperty<StringConverter<T>> converterProperty() { return converter; }
    private ObjectProperty<StringConverter<T>> converter = 
            new SimpleObjectProperty<StringConverter<T>>(this, "converter", ComboBox.<T>defaultStringConverter());
    public final void setConverter(StringConverter<T> value) { converterProperty().set(value); }
    public final StringConverter<T> getConverter() {return converterProperty().get(); }
    
    
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
     * The selection model for the ComboBox. In general a ComboBox supports only
     * single selection, but this is not necessarily always the case. Because of this,
     * the selection model in ComboBox is of type {@link SelectionModel}, which
     * means that the actual implementation may be SelectionModel, or a subclass
     * (such as {@link SingleSelectionModel} or {@link MultipleSelectionModel}).
     */
    private ObjectProperty<SelectionModel<T>> selectionModel 
            = new SimpleObjectProperty<SelectionModel<T>>(this, "selectionModel");
    public final void setSelectionModel(SelectionModel<T> value) { selectionModel.set(value); }
    public final SelectionModel<T> getSelectionModel() { return selectionModel.get(); }
    public final ObjectProperty<SelectionModel<T>> selectionModelProperty() { return selectionModel; }
    
    
    // --- Visible Row Count
    /**
     * The maximum number of rows to be visible in the ComboBox popup when it is
     * showing. By default this value is 10, but this can be changed to increase
     * or decrease the height of the popup.
     */
    private IntegerProperty visibleRowCount
            = new SimpleIntegerProperty(this, "visibleRowCount", 10);
    public final void setVisibleRowCount(int value) { visibleRowCount.set(value); }
    public final int getVisibleRowCount() { return visibleRowCount.get(); }
    public final IntegerProperty visibleRowCountProperty() { return visibleRowCount; }
    
    
    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    private static <T> StringConverter<T> defaultStringConverter() {
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

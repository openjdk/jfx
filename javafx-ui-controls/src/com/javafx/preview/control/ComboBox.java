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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Cell;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.Callback;
import javafx.util.StringConverter;

/*
 * TODO:
 *   * Auto-complete support when user typing
 *   * Proper CSS styling for mouse and keyboard navigation / selection
 *   * ComboBox grows/shrinks when selection changes
 *   * Mouse wheel scrolling doesn't work in listview popup
 *   * ListView width isn't correct - it needs to be max width of all items.
 *   * Add support for specifying the maximum number of visible rows in the list
 */
public class ComboBox<T> extends Control {
    
    public ComboBox() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setItems(FXCollections.<T>observableArrayList());
        setSelectionModel(new ComboBoxSelectionModel<T>(this));
    }
    
    
    // --- value
    /**
     * The value of this ComboBox is defined as the selected item if the input
     * is not editable, or if it is editable, the most recent user action: 
     * either the text input they have provided (converted via the 
     * StringConverter), or the last item selected from the drop down list.
     */
    public ObjectProperty<T> valueProperty() { return value; }
    private ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
    public final void setValue(T value) { valueProperty().set(value); }
    public final T getValue() { return valueProperty().get(); }
    
    
    // --- editable
    /**
     * Specifies whether the ComboBox allows for user input. When editable is 
     * true, the ComboBox has a text input area that a user may type in to. This
     * input is then available via the {@link #valueProperty() value} property.
     */
    public BooleanProperty editableProperty() { return editable; }
    public final void setEditable(boolean value) { editableProperty().set(value); }
    public final boolean isEditable() { return editableProperty().get(); }
    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE);
        }
    };
    
    
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
    
    
    // --- showing
    /**
     * Represents the current state of the ComboBox popup, and whether it is 
     * currently visible on screen (although it may be hidden behind other windows).
     */
    public BooleanProperty showingProperty() { return showing; }
    public final boolean isShowing() { return showingProperty().get(); }
    private BooleanProperty showing = new SimpleBooleanProperty(this, "showing", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_SHOWING);
        }
    };
    
    
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
    
    
//    // --- popup height
//    private DoubleProperty popupHeight = new SimpleDoubleProperty(this, "popupHeight", USE_COMPUTED_SIZE);
//    public final void setPopupHeight(double value) { popupHeightProperty().set(value); }
//    public final double getPopupHeight() {return popupHeightProperty().get(); }
//    public DoubleProperty popupHeightProperty() { return popupHeight; }
    
    
    // --- armed
    public BooleanProperty armedProperty() { return armed; }
    private final void setArmed(boolean value) { armedProperty().set(value); }
    public final boolean isArmed() { return armedProperty().get(); }
    private BooleanProperty armed = new SimpleBooleanProperty(this, "armed", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_ARMED);
        }
    };
    
    
    /**
     * The selection model for the ChoiceBox. Only a single choice can be made,
     * hence, the ChoiceBox supports only a SingleSelectionModel. Generally, the
     * main interaction with the selection model is to explicitly set which item
     * in the items list should be selected, or to listen to changes in the
     * selection to know which item has been chosen.
     */
    private ObjectProperty<SingleSelectionModel<T>> selectionModel 
            = new SimpleObjectProperty<SingleSelectionModel<T>>(this, "selectionModel");
    private final void setSelectionModel(SingleSelectionModel<T> value) { selectionModel.set(value); }
    public final SingleSelectionModel<T> getSelectionModel() { return selectionModel.get(); }
    public final ObjectProperty<SingleSelectionModel<T>> selectionModelProperty() { return selectionModel; }
    
    
    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Opens the list popup.
     */
    public void show() {
        if (!isDisabled()) showing.set(true);
    }

    /**
     * Closes the list popup.
     */
    public void hide() {
        showing.set(false);
    }
    
    /**
     * Arms the ComboBox. An armed ComboBox will show a popup list on the next 
     * expected UI gesture.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void arm() {
        setArmed(true);
    }

    /**
     * Disarms the ComboBox. See {@link #arm()}.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void disarm() {
        setArmed(false);
    }
    
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
    
    private static final String PSEUDO_CLASS_EDITABLE = "editable";
    private static final String PSEUDO_CLASS_SHOWING = "showing";
    private static final String PSEUDO_CLASS_ARMED = "armed";
    
    private static final long PSEUDO_CLASS_EDITABLE_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_EDITABLE);
    private static final long PSEUDO_CLASS_SHOWING_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_SHOWING);
    private static final long PSEUDO_CLASS_ARMED_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_ARMED);
    
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isEditable()) mask |= PSEUDO_CLASS_EDITABLE_MASK;
        if (isShowing()) mask |= PSEUDO_CLASS_SHOWING_MASK;
        if (isArmed()) mask |= PSEUDO_CLASS_ARMED_MASK;
        return mask;
    }
    
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

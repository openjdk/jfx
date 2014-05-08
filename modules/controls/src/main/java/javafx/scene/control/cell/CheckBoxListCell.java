/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link ListCell} implementation that draws a 
 * {@link CheckBox} node inside the cell, optionally with a label to indicate 
 * what the checkbox represents.
 * 
 * <p>The CheckBoxListCell is rendered with a CheckBox on the left-hand side of 
 * the {@link ListView}, and the text related to the list item taking up all 
 * remaining horizontal space. 
 * 
 * <p>To construct an instance of this class, it is necessary to provide a 
 * {@link Callback} that, given an object of type T, will return a 
 * {@code ObservableValue<Boolean>} that represents whether the given item is 
 * selected or not. This ObservableValue will be bound bidirectionally (meaning 
 * that the CheckBox in the cell will set/unset this property based on user 
 * interactions, and the CheckBox will reflect the state of the 
 * ObservableValue<Boolean>, if it changes externally).
 * 
 * @see CheckBox
 * @see ListCell
 * @param <T> The type of the elements contained within the ListView.
 * @since JavaFX 2.2
 */
public class CheckBoxListCell<T> extends ListCell<T> {
    
    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/    
    
    /**
     * Creates a cell factory for use in ListView controls. When used in a 
     * ListView, the {@link CheckBoxListCell} is rendered with a CheckBox on the 
     * left-hand side of the ListView, with the text related to the list item 
     * taking up all remaining horizontal space. 
     * 
     * @param <T> The type of the elements contained within the ListView.
     * @param getSelectedProperty A {@link Callback} that, given an object of 
     *      type T (which is a value taken out of the 
     *      {@code ListView<T>.items} list), 
     *      will return an {@code ObservableValue<Boolean>} that represents 
     *      whether the given item is selected or not. This ObservableValue will 
     *      be bound bidirectionally (meaning that the CheckBox in the cell will 
     *      set/unset this property based on user interactions, and the CheckBox 
     *      will reflect the state of the ObservableValue, if it changes 
     *      externally).
     * @return A {@link Callback} that will return a ListCell that is able to 
     *      work on the type of element contained within the ListView items list.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty) {
        return forListView(getSelectedProperty, CellUtils.<T>defaultStringConverter());
    }
    
    /**
     * Creates a cell factory for use in ListView controls. When used in a 
     * ListView, the {@link CheckBoxListCell} is rendered with a CheckBox on the
     * left-hand side of the ListView, with the text related to the list item 
     * taking up all remaining horizontal space. 
     * 
     * @param <T> The type of the elements contained within the ListView.
     * @param getSelectedProperty A {@link Callback} that, given an object 
     *      of type T (which is a value taken out of the 
     *      {@code ListView<T>.items} list), 
     *      will return an {@code ObservableValue<Boolean>} that represents 
     *      whether the given item is selected or not. This ObservableValue will 
     *      be bound bidirectionally (meaning that the CheckBox in the cell will 
     *      set/unset this property based on user interactions, and the CheckBox 
     *      will reflect the state of the ObservableValue, if it changes 
     *      externally).
     * @param converter A StringConverter that, give an object of type T, will 
     *      return a String that can be used to represent the object visually. 
     * @return A {@link Callback} that will return a ListCell that is able to 
     *      work on the type of element contained within the ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<T> converter) {
        return list -> new CheckBoxListCell<T>(getSelectedProperty, converter);
    }
    
    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/
    
    private final CheckBox checkBox;
    
    private ObservableValue<Boolean> booleanProperty;
    

    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Creates a default CheckBoxListCell.
     */
    public CheckBoxListCell() { 
        this(null);
    } 
    
    /**
     * Creates a default CheckBoxListCell.
     * 
     * @param getSelectedProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} given an item from the ListView.
     */
    public CheckBoxListCell(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty) {
        this(getSelectedProperty, CellUtils.<T>defaultStringConverter());
    }
    
    /**
     * Creates a CheckBoxListCell with a custom string converter.
     * 
     * @param getSelectedProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} given an item from the ListView.
     * @param converter A StringConverter that, given an object of type T, will 
     *      return a String that can be used to represent the object visually.
     */
    public CheckBoxListCell(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<T> converter) {
        this.getStyleClass().add("check-box-list-cell");
        setSelectedStateCallback(getSelectedProperty);
        setConverter(converter);
        
        this.checkBox = new CheckBox();
        
        setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(ContentDisplay.LEFT);

        // by default the graphic is null until the cell stops being empty
        setGraphic(null);
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    // --- converter
    private ObjectProperty<StringConverter<T>> converter = 
            new SimpleObjectProperty<StringConverter<T>>(this, "converter");

    /**
     * The {@link StringConverter} property.
     */
    public final ObjectProperty<StringConverter<T>> converterProperty() { 
        return converter; 
    }
    
    /** 
     * Sets the {@link StringConverter} to be used in this cell.
     */
    public final void setConverter(StringConverter<T> value) { 
        converterProperty().set(value); 
    }
    
    /**
     * Returns the {@link StringConverter} used in this cell.
     */
    public final StringConverter<T> getConverter() { 
        return converterProperty().get(); 
    }
    
    
    // --- selected state callback property
    private ObjectProperty<Callback<T, ObservableValue<Boolean>>> 
            selectedStateCallback = 
            new SimpleObjectProperty<Callback<T, ObservableValue<Boolean>>>(
            this, "selectedStateCallback");

    /**
     * Property representing the {@link Callback} that is bound to by the 
     * CheckBox shown on screen.
     */
    public final ObjectProperty<Callback<T, ObservableValue<Boolean>>> selectedStateCallbackProperty() { 
        return selectedStateCallback; 
    }
    
    /** 
     * Sets the {@link Callback} that is bound to by the CheckBox shown on screen.
     */
    public final void setSelectedStateCallback(Callback<T, ObservableValue<Boolean>> value) { 
        selectedStateCallbackProperty().set(value); 
    }
    
    /**
     * Returns the {@link Callback} that is bound to by the CheckBox shown on screen.
     */
    public final Callback<T, ObservableValue<Boolean>> getSelectedStateCallback() { 
        return selectedStateCallbackProperty().get(); 
    }

    

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        
        if (! empty) {
            StringConverter c = getConverter();
            Callback<T, ObservableValue<Boolean>> callback = getSelectedStateCallback();
            if (callback == null) {
                throw new NullPointerException(
                        "The CheckBoxListCell selectedStateCallbackProperty can not be null");
            }
            
            setGraphic(checkBox);
            setText(c != null ? c.toString(item) : (item == null ? "" : item.toString()));
            
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            booleanProperty = callback.call(item);
            if (booleanProperty != null) {
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
            }
        } else {
            setGraphic(null);
            setText(null);
        }
    }
}

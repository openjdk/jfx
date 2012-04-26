/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
 * @see CheckBoxCellFactory
 * @see CheckBox
 * @see ListCell
 * @param <T> The type of the elements contained within the ListView.
 */
public class CheckBoxListCell<T> extends ListCell<T> {
    private final CheckBox checkBox;
    
    private final StringConverter<T> converter;

    private final Callback<T, ObservableValue<Boolean>> getSelectedProperty;
    private ObservableValue<Boolean> booleanProperty;
    
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
        if (getSelectedProperty == null) {
            throw new NullPointerException("getSelectedProperty can not be null");
        }
        this.getSelectedProperty = getSelectedProperty;
        this.converter = converter;
        
        this.checkBox = new CheckBox();
        
        setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(ContentDisplay.LEFT);
        setGraphic(checkBox);
    }
    
    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        
        if (! empty) {
            setGraphic(checkBox);
            setText(converter != null ? 
                    converter.toString(item) : (item == null ? "" : item.toString()));
            
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            booleanProperty = getSelectedProperty.call(item);
            if (booleanProperty != null) {
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
            }
        } else {
            setGraphic(null);
            setText(null);
        }
    }
}